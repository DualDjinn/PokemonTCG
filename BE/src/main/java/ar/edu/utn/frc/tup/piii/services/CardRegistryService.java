package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.configs.PokemonApiProperties;
import ar.edu.utn.frc.tup.piii.dtos.CardApiDTO;
import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.CardRarity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.InputStream;
import java.util.*;

/**
 * In-memory registry of all card domain objects and raw API DTOs.
 *
 * <p>At startup, loads cards from disk cache files (written by
 * {@link CardApiSyncService}) and from the hardcoded
 * {@code pokesecret.json}. Falls back to {@code db.json} if
 * no cache file exists yet (first-ever startup before any API call).
 */
@Component
public class CardRegistryService {

    private static final Logger LOG = LoggerFactory.getLogger(CardRegistryService.class);
    private static final String DB_JSON_PATH = "classpath:data/db.json";
    private static final String POKESECRET_JSON_PATH = "classpath:data/pokesecret.json";
    private static final String CACHE_DIR = "data";
    private static final String CACHE_PREFIX = "api_cache_";

    private final CardMapper cardMapper;
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;
    private final PokemonApiProperties apiProperties;

    private volatile Map<String, Card> registry;
    private volatile List<CardApiDTO> rawCards;

    /**
     * Constructs the registry with the required dependencies.
     *
     * @param cardMapper      maps CardApiDTO to domain Card
     * @param objectMapper    Jackson ObjectMapper
     * @param resourceLoader  Spring resource loader
     * @param apiProperties   Pokemon API configuration
     */
    public CardRegistryService(CardMapper cardMapper, ObjectMapper objectMapper,
                               ResourceLoader resourceLoader,
                               PokemonApiProperties apiProperties) {
        this.cardMapper = cardMapper;
        this.objectMapper = objectMapper;
        this.resourceLoader = resourceLoader;
        this.apiProperties = apiProperties;
    }

    /**
     * Initializes the registry at startup by loading from disk cache
     * files (if available) or falling back to db.json, plus pokesecret.json.
     */
    @PostConstruct
    public void init() {
        List<CardApiDTO> allDtos = new ArrayList<>();
        Map<String, Card> map = new LinkedHashMap<>();

        boolean cacheLoaded = false;
        for (String setId : apiProperties.getSetIds()) {
            File cacheFile = getCacheFile(setId);
            if (cacheFile.exists()) {
                loadFileFromPath(cacheFile, allDtos, map);
                cacheLoaded = true;
            }
        }

        if (!cacheLoaded) {
            loadClasspathFile(DB_JSON_PATH, allDtos, map);
        }

        loadClasspathFile(POKESECRET_JSON_PATH, allDtos, map);

        this.rawCards = Collections.unmodifiableList(allDtos);
        this.registry = Collections.unmodifiableMap(map);
        LOG.info("Loaded {} cards into registry.", map.size());
    }

    /**
     * Reloads the registry from disk cache files plus pokesecret.json.
     * Called by CardApiSyncService after writing new cache files.
     */
    public void reloadFromDiskCache() {
        List<CardApiDTO> allDtos = new ArrayList<>();
        Map<String, Card> map = new LinkedHashMap<>();

        for (String setId : apiProperties.getSetIds()) {
            File cacheFile = getCacheFile(setId);
            if (cacheFile.exists()) {
                loadFileFromPath(cacheFile, allDtos, map);
            }
        }

        loadClasspathFile(POKESECRET_JSON_PATH, allDtos, map);

        this.rawCards = Collections.unmodifiableList(allDtos);
        this.registry = Collections.unmodifiableMap(map);
        LOG.info("Reloaded {} cards into registry from disk cache.", map.size());
    }

    /**
     * Loads cards directly from a list of API DTOs into the registry.
     * Used by CardApiSyncService for immediate in-memory refresh
     * after an API fetch.
     *
     * @param apiDtos the raw DTOs from the API
     */
    public void reloadFromDtos(List<CardApiDTO> apiDtos) {
        List<CardApiDTO> allDtos = new ArrayList<>();
        Map<String, Card> map = new LinkedHashMap<>();

        addDtosToRegistry(apiDtos, allDtos, map);
        loadClasspathFile(POKESECRET_JSON_PATH, allDtos, map);

        this.rawCards = Collections.unmodifiableList(allDtos);
        this.registry = Collections.unmodifiableMap(map);
        LOG.info("Reloaded {} cards into registry from API DTOs.", map.size());
    }

    /**
     * Loads cards from the fallback db.json classpath resource.
     * Used by CardApiSyncService when the API is unreachable on first startup.
     *
     * @return list of CardApiDTOs loaded from db.json
     */
    public List<CardApiDTO> loadFallbackJson() {
        List<CardApiDTO> dtos = new ArrayList<>();
        Map<String, Card> map = new LinkedHashMap<>();
        loadClasspathFile(DB_JSON_PATH, dtos, map);
        return dtos;
    }

    /**
     * Returns the domain card for the given ID.
     *
     * @param id the card identifier
     * @return the Card
     * @throws IllegalArgumentException if the card is not found
     */
    public Card getById(String id) {
        Card card = registry.get(id);
        if (card == null) {
            throw new IllegalArgumentException("Card not found: " + id);
        }
        return card;
    }

    /**
     * Returns the domain card for the given ID, if present.
     *
     * @param id the card identifier
     * @return optional containing the card
     */
    public Optional<Card> findById(String id) {
        return Optional.ofNullable(registry.get(id));
    }

    /**
     * Returns all domain cards in the registry.
     *
     * @return collection of all cards
     */
    public Collection<Card> getAll() {
        return registry.values();
    }

    /**
     * Returns the raw API DTOs for all loaded cards.
     *
     * @return unmodifiable list of CardApiDTO
     */
    public List<CardApiDTO> getRawCards() {
        return rawCards;
    }

    /**
     * Returns the cache file path for a given set ID.
     *
     * @param setId the set identifier
     * @return the cache file
     */
    File getCacheFile(String setId) {
        return new File(CACHE_DIR, CACHE_PREFIX + setId + ".json");
    }

    private void loadClasspathFile(String path, List<CardApiDTO> allDtos,
                                   Map<String, Card> map) {
        try {
            Resource resource = resourceLoader.getResource(path);
            InputStream is = resource.getInputStream();
            JsonNode root = objectMapper.readTree(is);
            JsonNode dataNode = root.get("data");
            CardApiDTO[] dtos = objectMapper.treeToValue(dataNode, CardApiDTO[].class);
            addDtosToRegistry(List.of(dtos), allDtos, map);
            LOG.info("Loaded cards from classpath: {}", path);
        } catch (Exception e) {
            LOG.warn("Failed to load cards from {}: {}", path, e.getMessage());
        }
    }

    private void loadFileFromPath(File file, List<CardApiDTO> allDtos,
                                  Map<String, Card> map) {
        try {
            JsonNode root = objectMapper.readTree(file);
            JsonNode dataNode = root.get("data");
            CardApiDTO[] dtos = objectMapper.treeToValue(dataNode, CardApiDTO[].class);
            addDtosToRegistry(List.of(dtos), allDtos, map);
            LOG.info("Loaded cards from cache: {}", file.getPath());
        } catch (Exception e) {
            LOG.warn("Failed to load cards from {}: {}", file.getPath(), e.getMessage());
        }
    }

    private void addDtosToRegistry(List<CardApiDTO> dtos, List<CardApiDTO> allDtos,
                                   Map<String, Card> map) {
        int skipped = 0;
        for (CardApiDTO dto : dtos) {
            if (!"Rare Ultra".equals(dto.getRarity())) {
                allDtos.add(dto);
            }
            try {
                Card card = cardMapper.toDomain(dto);
                if (card != null && card.getRarity() == CardRarity.RARE_ULTRA) {
                    skipped++;
                } else if (card != null) {
                    map.put(card.getId(), card);
                } else {
                    skipped++;
                }
            } catch (Exception e) {
                LOG.warn("Skipping card {} ({}): {}",
                        dto.getId(), dto.getName(), e.getMessage());
                skipped++;
            }
        }
        if (skipped > 0) {
            LOG.info("Skipped {} cards during registry load.", skipped);
        }
    }
}
