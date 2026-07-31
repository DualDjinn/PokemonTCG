package ar.edu.utn.frc.tup.piii.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ar.edu.utn.frc.tup.piii.dtos.cardback.CardBackDTO;
import ar.edu.utn.frc.tup.piii.dtos.cardback.PlayerCardBacksDTO;
import ar.edu.utn.frc.tup.piii.models.CardBack;
import ar.edu.utn.frc.tup.piii.models.CardBackOrigin;
import ar.edu.utn.frc.tup.piii.models.Player;
import ar.edu.utn.frc.tup.piii.models.PlayerCardBack;
import ar.edu.utn.frc.tup.piii.repositories.CardBackRepository;
import ar.edu.utn.frc.tup.piii.repositories.MatchRepository;
import ar.edu.utn.frc.tup.piii.repositories.PlayerCardBackRepository;
import ar.edu.utn.frc.tup.piii.repositories.PlayerRepository;

/**
 * Dorsos de carta cosméticos: catálogo, compra, otorgamiento y equipado.
 *
 * <p>El dorso equipado es global por jugador ({@code Player.activeCardBackId});
 * null significa el dorso clásico por defecto, que todos poseen implícitamente.
 */
@Service
public class CardBackService {

    private final CardBackRepository cardBackRepository;
    private final PlayerCardBackRepository playerCardBackRepository;
    private final PlayerRepository playerRepository;
    private final MatchRepository matchRepository;
    private final IdempotencyService idempotencyService;
    private final Random random = new Random();

    public CardBackService(CardBackRepository cardBackRepository,
                           PlayerCardBackRepository playerCardBackRepository,
                           PlayerRepository playerRepository,
                           MatchRepository matchRepository,
                           IdempotencyService idempotencyService) {
        this.cardBackRepository = cardBackRepository;
        this.playerCardBackRepository = playerCardBackRepository;
        this.playerRepository = playerRepository;
        this.matchRepository = matchRepository;
        this.idempotencyService = idempotencyService;
    }

    /** Catálogo completo de dorsos. */
    public List<CardBackDTO> getCatalog() {
        return cardBackRepository.findAll().stream().map(this::toDto).toList();
    }

    /**
     * Dorsos que posee el jugador + cuál tiene equipado. Antes de responder
     * sincroniza los desbloqueos por progreso (batallas ganadas / nivel),
     * así el jugador siempre ve lo que ya se ganó.
     */
    public PlayerCardBacksDTO getPlayerBacks(String playerId) {
        syncProgressUnlocks(playerId);
        Player player = getPlayerOrThrow(playerId);
        List<CardBackDTO> owned = playerCardBackRepository.findByPlayer_Id(playerId).stream()
                .map(pcb -> toDto(pcb.getCardBack()))
                .toList();
        return new PlayerCardBacksDTO(player.getActiveCardBackId(), owned);
    }

    /**
     * Otorga los dorsos de origen DEFAULT (todos los jugadores) y los de
     * BATTLE/LEVEL cuyo umbral el jugador ya alcanzó (batallas ganadas o
     * nivel actual). Idempotente.
     */
    @Transactional
    public void syncProgressUnlocks(String playerId) {
        Player player = playerRepository.findById(playerId).orElse(null);
        if (player == null || player.isBot()) {
            return;
        }
        long wins = matchRepository.countByWinnerId(playerId);
        int level = player.getLevel();
        for (CardBack back : cardBackRepository.findAll()) {
            Integer threshold = back.getUnlockValue();
            boolean earned = switch (back.getOrigin()) {
                case DEFAULT -> true;
                case BATTLE -> threshold != null && wins >= threshold;
                case LEVEL -> threshold != null && level >= threshold;
                default -> false;
            };
            if (earned) {
                grant(playerId, back.getId());
            }
        }
    }

    /**
     * Sortea un dorso de origen PACK que el jugador no posea y se lo otorga.
     *
     * @return el dorso otorgado, o null si ya posee todos los de sobre
     */
    @Transactional
    public CardBackDTO dropRandomPackCardBack(String playerId) {
        Set<String> ownedIds = playerCardBackRepository.findByPlayer_Id(playerId).stream()
                .map(pcb -> pcb.getCardBack().getId())
                .collect(Collectors.toSet());
        List<CardBack> candidates = cardBackRepository.findAll().stream()
                .filter(back -> back.getOrigin() == CardBackOrigin.PACK)
                .filter(back -> !ownedIds.contains(back.getId()))
                .toList();
        if (candidates.isEmpty()) {
            return null;
        }
        CardBack dropped = candidates.get(random.nextInt(candidates.size()));
        grant(playerId, dropped.getId());
        return toDto(dropped);
    }

    /**
     * Buys a shop card back idempotently: validates SHOP origin, that it is not already
     * owned and that the player has enough coins; debits and grants it. With the same
     * {@code Idempotency-Key}, a replay returns the cached result without charging again.
     *
     * @param playerId       authenticated player ID
     * @param cardBackId     ID of the card back to buy
     * @param idempotencyKey {@code Idempotency-Key} header value; null = no deduplication
     */
    public PlayerCardBacksDTO purchase(String playerId, String cardBackId, String idempotencyKey) {
        return idempotencyService.execute(idempotencyKey, playerId, "cardback:purchase",
                PlayerCardBacksDTO.class, () -> doPurchase(playerId, cardBackId));
    }

    private PlayerCardBacksDTO doPurchase(String playerId, String cardBackId) {
        Player player = getPlayerOrThrow(playerId);
        CardBack back = cardBackRepository.findById(cardBackId)
                .orElseThrow(() -> new IllegalArgumentException("Card back not found: " + cardBackId));

        if (back.getOrigin() != CardBackOrigin.SHOP || back.getPrice() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Card back is not for sale: " + cardBackId);
        }
        if (playerCardBackRepository.existsByPlayer_IdAndCardBack_Id(playerId, cardBackId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Card back already owned: " + cardBackId);
        }
        if (player.getCoins() < back.getPrice()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient coins");
        }

        player.setCoins(player.getCoins() - back.getPrice());
        playerRepository.save(player);
        grant(playerId, cardBackId);
        return getPlayerBacks(playerId);
    }

    /**
     * Otorga un dorso al jugador (sobres, batallas, niveles). Idempotente:
     * si ya lo posee no hace nada.
     *
     * @return true si se otorgó ahora, false si ya lo tenía
     */
    @Transactional
    public boolean grant(String playerId, String cardBackId) {
        if (playerCardBackRepository.existsByPlayer_IdAndCardBack_Id(playerId, cardBackId)) {
            return false;
        }
        Player player = getPlayerOrThrow(playerId);
        CardBack back = cardBackRepository.findById(cardBackId)
                .orElseThrow(() -> new IllegalArgumentException("Card back not found: " + cardBackId));

        PlayerCardBack ownership = new PlayerCardBack();
        ownership.setPlayer(player);
        ownership.setCardBack(back);
        ownership.setAcquiredAt(LocalDateTime.now());
        playerCardBackRepository.save(ownership);
        return true;
    }

    /**
     * Equipa un dorso como dorso global del jugador. Debe poseerlo;
     * {@code null} restaura el dorso clásico por defecto.
     */
    @Transactional
    public PlayerCardBacksDTO equip(String playerId, String cardBackId) {
        Player player = getPlayerOrThrow(playerId);
        if (cardBackId != null
                && !playerCardBackRepository.existsByPlayer_IdAndCardBack_Id(playerId, cardBackId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Player does not own card back: " + cardBackId);
        }
        player.setActiveCardBackId(cardBackId);
        playerRepository.save(player);
        return getPlayerBacks(playerId);
    }

    /** URL del dorso equipado del jugador, o null si usa el de por defecto. */
    public String getActiveCardBackUrl(String playerId) {
        return playerRepository.findById(playerId)
                .map(Player::getActiveCardBackId)
                .flatMap(cardBackRepository::findById)
                .map(CardBack::getImageUrl)
                .orElse(null);
    }

    private Player getPlayerOrThrow(String playerId) {
        return playerRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Player not found"));
    }

    private CardBackDTO toDto(CardBack back) {
        return new CardBackDTO(
                back.getId(),
                back.getName(),
                back.getImageUrl(),
                back.getOrigin().name(),
                back.getPrice(),
                back.getUnlockValue());
    }
}
