package ar.edu.utn.frc.tup.piii.services;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import ar.edu.utn.frc.tup.piii.configs.PokemonApiProperties;
import ar.edu.utn.frc.tup.piii.dtos.CardApiDTO;
import ar.edu.utn.frc.tup.piii.dtos.CardApiResponseDTO;
import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.CardRarity;
import ar.edu.utn.frc.tup.piii.models.CardSetFetchLog;
import ar.edu.utn.frc.tup.piii.models.CollectibleCard;
import ar.edu.utn.frc.tup.piii.models.EnergyCard;
import ar.edu.utn.frc.tup.piii.models.FetchStatus;
import ar.edu.utn.frc.tup.piii.models.PokemonCard;
import ar.edu.utn.frc.tup.piii.models.TrainerCard;
import ar.edu.utn.frc.tup.piii.repositories.CardRepository;
import ar.edu.utn.frc.tup.piii.repositories.CardSetFetchLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates periodic synchronisation of card data from the Pokemon TCG API.
 *
 * <p>Runs on a configurable cron schedule and at application startup (when the
 * database is empty). Fetches card data, upserts into the database, writes a
 * disk cache for fast restarts, and refreshes in-memory caches.
 */
@Service
@Order(0)
public class CardApiSyncService implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(CardApiSyncService.class);
    private static final int DAYS_BETWEEN_SYNCS = 7;

    private final PokemonTcgApiClient apiClient;
    private final CardMapper cardMapper;
    private final CardRepository cardRepository;
    private final CardSetFetchLogRepository fetchLogRepository;
    private final CardRegistryService cardRegistryService;
    private final CardService cardService;
    private final PokemonApiProperties apiProperties;
    private final ObjectMapper objectMapper;

    private final AtomicBoolean syncing = new AtomicBoolean(false);

    /**
     * Constructs the sync service with its dependencies.
     *
     * @param apiClient            the HTTP client for the Pokemon TCG API
     * @param cardMapper           maps CardApiDTO to domain Card
     * @param cardRepository       JPA repository for Card entities
     * @param fetchLogRepository   JPA repository for fetch log entries
     * @param cardRegistryService  in-memory card registry
     * @param cardService          card service for REST facade
     * @param apiProperties        API configuration properties
     * @param objectMapper         Jackson ObjectMapper for JSON serialization
     */
    public CardApiSyncService(PokemonTcgApiClient apiClient,
                              CardMapper cardMapper,
                              CardRepository cardRepository,
                              CardSetFetchLogRepository fetchLogRepository,
                              CardRegistryService cardRegistryService,
                              CardService cardService,
                              PokemonApiProperties apiProperties,
                              ObjectMapper objectMapper) {
        this.apiClient = apiClient;
        this.cardMapper = cardMapper;
        this.cardRepository = cardRepository;
        this.fetchLogRepository = fetchLogRepository;
        this.cardRegistryService = cardRegistryService;
        this.cardService = cardService;
        this.apiProperties = apiProperties;
        this.objectMapper = objectMapper;
    }

    /**
     * Handles first-startup seeding when the database is empty.
     * Falls back to db.json if the API is unreachable.
     * Always ensures pokesecret cards are persisted, even on subsequent startups.
     */
    @Override
    public void run(ApplicationArguments args) {
        if (cardRepository.count() > 0) {
            LOG.info("Database already has cards, skipping startup sync.");
            seedPokesecretCards();
            return;
        }

        LOG.info("Database is empty, performing initial card sync...");
        List<CardApiDTO> allDtos = new ArrayList<>();

        for (String setId : apiProperties.getSetIds()) {
            SyncResult result = syncSet(setId);
            if (result.status == FetchStatus.FAILED) {
                LOG.warn("API unreachable for set {}. Falling back to db.json.", setId);
                List<CardApiDTO> fallbackDtos = cardRegistryService.loadFallbackJson();
                if (!fallbackDtos.isEmpty()) {
                    persistDtos(fallbackDtos);
                    allDtos.addAll(fallbackDtos);
                    writeDiskCache(setId, fallbackDtos);
                    logFetch(setId, fallbackDtos.size(), fallbackDtos.size(),
                            0, FetchStatus.SUCCESS, null);
                }
            } else {
                allDtos.addAll(result.dtos);
            }
        }

        if (!allDtos.isEmpty()) {
            cardRegistryService.reloadFromDtos(allDtos);
            cardService.refreshCache();
        }
        seedPokesecretCards();
    }

    /**
     * Ensures all pokesecret cards from the in-memory registry are persisted
     * in the database. Runs on every startup so existing databases receive
     * pokesecret cards even if they were not present when the DB was first seeded.
     */
    private void seedPokesecretCards() {
        cardRegistryService.getAll().stream()
                .filter(card -> card.getId().startsWith("pokesecret-"))
                .filter(card -> !cardRepository.existsById(card.getId()))
                .forEach(card -> {
                    cardRepository.save(card);
                    LOG.info("Seeded pokesecret card: {}", card.getId());
                });
    }

    /**
     * Scheduled cron job that checks each configured set and syncs if
     * more than 7 days have passed since the last successful fetch.
     */
    @Scheduled(cron = "${app.pokemon-api.sync-cron}")
    public void scheduledSync() {
        LOG.info("Running scheduled card sync...");
        boolean anyUpdated = false;

        for (String setId : apiProperties.getSetIds()) {
            if (shouldSync(setId)) {
                SyncResult result = syncSet(setId);
                if (result.status == FetchStatus.SUCCESS) {
                    anyUpdated = true;
                }
            } else {
                LOG.info("Set {} is up to date, skipping.", setId);
            }
        }

        if (anyUpdated) {
            cardRegistryService.reloadFromDiskCache();
            cardService.refreshCache();
            LOG.info("In-memory caches refreshed after scheduled sync.");
        }
    }

    /**
     * Synchronises a single card set from the API.
     *
     * @param setId the set identifier to sync
     * @return the result of the sync operation
     */
    SyncResult syncSet(String setId) {
        if (!syncing.compareAndSet(false, true)) {
            LOG.warn("Sync already in progress, skipping set {}", setId);
            return new SyncResult(FetchStatus.FAILED, List.of());
        }

        try {
            LOG.info("Starting sync for set {}", setId);
            List<CardApiDTO> apiDtos = apiClient.fetchAllCardsForSet(setId);

            if (apiDtos.isEmpty()) {
                String msg = "API returned empty result for set " + setId;
                LOG.warn(msg);
                logFetch(setId, 0, 0, 0, FetchStatus.FAILED, msg);
                return new SyncResult(FetchStatus.FAILED, apiDtos);
            }

            int[] counts = upsertCards(apiDtos);
            writeDiskCache(setId, apiDtos);
            logFetch(setId, apiDtos.size(), counts[0], counts[1],
                    FetchStatus.SUCCESS, null);

            LOG.info("Sync complete for set {}: {} total, {} inserted, {} updated",
                    setId, apiDtos.size(), counts[0], counts[1]);

            return new SyncResult(FetchStatus.SUCCESS, apiDtos);
        } catch (Exception e) {
            LOG.error("Sync failed for set {}: {}", setId, e.getMessage(), e);
            logFetch(setId, 0, 0, 0, FetchStatus.FAILED, e.getMessage());
            return new SyncResult(FetchStatus.FAILED, List.of());
        } finally {
            syncing.set(false);
        }
    }

    private boolean shouldSync(String setId) {
        Optional<CardSetFetchLog> lastSuccess = fetchLogRepository
                .findTopBySetIdAndStatusOrderByFetchedAtDesc(setId, FetchStatus.SUCCESS);

        if (lastSuccess.isEmpty()) {
            return true;
        }

        LocalDateTime threshold = LocalDateTime.now().minusDays(DAYS_BETWEEN_SYNCS);
        return lastSuccess.get().getFetchedAt().isBefore(threshold);
    }

    /**
     * Upserts cards into the database.
     *
     * @param apiDtos the raw DTOs from the API
     * @return array of [inserted, updated] counts
     */
    @Transactional
    int[] upsertCards(List<CardApiDTO> apiDtos) {
        int inserted = 0;
        int updated = 0;

        for (CardApiDTO dto : apiDtos) {
            if ("Rare Ultra".equals(dto.getRarity())) {
                continue;
            }

            try {
                Card incoming = cardMapper.toDomain(dto);
                if (incoming == null) {
                    continue;
                }

                Optional<Card> existingOpt = cardRepository.findById(incoming.getId());

                if (existingOpt.isEmpty()) {
                    cardRepository.save(incoming);
                    inserted++;
                } else {
                    updateExistingCard(existingOpt.get(), incoming);
                    cardRepository.save(existingOpt.get());
                    updated++;
                }
            } catch (Exception e) {
                LOG.warn("Failed to upsert card {}: {}", dto.getId(), e.getMessage());
            }
        }

        return new int[]{inserted, updated};
    }

    private void updateExistingCard(Card existing, Card incoming) {
        existing.setName(incoming.getName());
        existing.setRarity(incoming.getRarity());
        existing.setImageUrl(incoming.getImageUrl());
        existing.setImageUrlLarge(incoming.getImageUrlLarge());
        existing.setSetCode(incoming.getSetCode());

        if (existing instanceof PokemonCard existingPk
                && incoming instanceof PokemonCard incomingPk) {
            updatePokemonCard(existingPk, incomingPk);
        } else if (existing instanceof EnergyCard existingE
                && incoming instanceof EnergyCard incomingE) {
            existingE.setEnergyType(incomingE.getEnergyType());
            existingE.setRules(incomingE.getRules());
        } else if (existing instanceof TrainerCard existingT
                && incoming instanceof TrainerCard incomingT) {
            existingT.setRules(incomingT.getRules());
            existingT.setArtist(incomingT.getArtist());
        } else if (existing instanceof CollectibleCard existingC
                && incoming instanceof CollectibleCard incomingC) {
            existingC.setRules(incomingC.getRules());
            existingC.setArtist(incomingC.getArtist());
        }
    }

    private void updatePokemonCard(PokemonCard existing, PokemonCard incoming) {
        existing.setHp(incoming.getHp());
        existing.setStage(incoming.getStage());
        existing.setEx(incoming.isEx());
        existing.setMega(incoming.isMega());
        existing.setPokemonType(incoming.getPokemonType());
        existing.setRetreatCost(incoming.getRetreatCost());
        existing.setEvolvesFrom(incoming.getEvolvesFrom());
        existing.setEvolvesTo(incoming.getEvolvesTo());
        existing.setRules(incoming.getRules());
        existing.setWeaknesses(incoming.getWeaknesses());
        existing.setResistances(incoming.getResistances());
        existing.setCostToAttack(incoming.getCostToAttack());
        existing.setGifUrl(incoming.getGifUrl());

        existing.getAttacks().clear();
        existing.getAttacks().addAll(incoming.getAttacks());

        existing.getAbilities().clear();
        existing.getAbilities().addAll(incoming.getAbilities());
    }

    @Transactional
    void persistDtos(List<CardApiDTO> dtos) {
        List<Card> cards = new ArrayList<>();
        for (CardApiDTO dto : dtos) {
            if ("Rare Ultra".equals(dto.getRarity())) {
                continue;
            }
            try {
                Card card = cardMapper.toDomain(dto);
                if (card != null && card.getRarity() != CardRarity.RARE_ULTRA) {
                    cards.add(card);
                }
            } catch (Exception e) {
                LOG.warn("Skipping card {} during fallback persist: {}",
                        dto.getId(), e.getMessage());
            }
        }
        cardRepository.saveAll(cards);
        LOG.info("Persisted {} cards from fallback.", cards.size());
    }

    private void writeDiskCache(String setId, List<CardApiDTO> dtos) {
        try {
            File cacheFile = cardRegistryService.getCacheFile(setId);
            cacheFile.getParentFile().mkdirs();
            CardApiResponseDTO wrapper = new CardApiResponseDTO();
            wrapper.setData(dtos);
            wrapper.setTotalCount(dtos.size());
            wrapper.setCount(dtos.size());
            wrapper.setPage(1);
            wrapper.setPageSize(dtos.size());
            objectMapper.writeValue(cacheFile, wrapper);
            LOG.info("Wrote disk cache for set {} ({} cards) to {}",
                    setId, dtos.size(), cacheFile.getPath());
        } catch (Exception e) {
            LOG.warn("Failed to write disk cache for set {}: {}", setId, e.getMessage());
        }
    }

    private void logFetch(String setId, int cardCount, int inserted,
                          int updated, FetchStatus status, String errorMessage) {
        CardSetFetchLog log = CardSetFetchLog.builder()
                .setId(setId)
                .fetchedAt(LocalDateTime.now())
                .cardCount(cardCount)
                .cardsInserted(inserted)
                .cardsUpdated(updated)
                .status(status)
                .errorMessage(errorMessage)
                .build();
        fetchLogRepository.save(log);
    }

    static class SyncResult {
        final FetchStatus status;
        final List<CardApiDTO> dtos;

        SyncResult(FetchStatus status, List<CardApiDTO> dtos) {
            this.status = status;
            this.dtos = dtos;
        }
    }
}
