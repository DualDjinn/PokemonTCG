package ar.edu.utn.frc.tup.piii.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import ar.edu.utn.frc.tup.piii.dtos.cardback.CardBackDTO;
import ar.edu.utn.frc.tup.piii.dtos.pack.OpenedPackResponse;
import ar.edu.utn.frc.tup.piii.dtos.pack.PackInfoDTO;
import ar.edu.utn.frc.tup.piii.dtos.player.CardSummaryDTO;
import ar.edu.utn.frc.tup.piii.dtos.skin.CardSkinDTO;
import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.CardRarity;
import ar.edu.utn.frc.tup.piii.models.CardSkin;
import ar.edu.utn.frc.tup.piii.models.PackPurchase;
import ar.edu.utn.frc.tup.piii.models.PackType;
import ar.edu.utn.frc.tup.piii.models.Player;
import ar.edu.utn.frc.tup.piii.models.PlayerCard;
import ar.edu.utn.frc.tup.piii.models.RarityBucket;
import ar.edu.utn.frc.tup.piii.models.Supertype;
import ar.edu.utn.frc.tup.piii.repositories.CardRepository;
import ar.edu.utn.frc.tup.piii.repositories.CardSkinRepository;
import ar.edu.utn.frc.tup.piii.repositories.PackPurchaseRepository;
import ar.edu.utn.frc.tup.piii.repositories.PlayerCardRepository;
import ar.edu.utn.frc.tup.piii.repositories.PlayerRepository;

/**
 * Servicio de tienda de sobres.
 *
 * <p>Orquesta la compra y apertura de un sobre en una sola transacción atómica:
 * valida saldo, descuenta monedas, sortea cartas, las agrega a la colección y
 * registra el histórico.
 */
@Service
public class PackService {

    /** Prefijo de IDs del set Pokesecret (cartas easter-egg obtenibles solo en
        el juego, no en sobres). */
    private static final String POKESECRET_ID_PREFIX = "pokesecret-";

    private final PlayerRepository playerRepository;
    private final CardRepository cardRepository;
    private final PlayerCardRepository playerCardRepository;
    private final PackPurchaseRepository packPurchaseRepository;
    private final PackRandomizer packRandomizer;
    private final CardSkinRepository cardSkinRepository;
    private final SkinService skinService;
    private final CardBackService cardBackService;
    private final IdempotencyService idempotencyService;

    public PackService(PlayerRepository playerRepository,
                       CardRepository cardRepository,
                       PlayerCardRepository playerCardRepository,
                       PackPurchaseRepository packPurchaseRepository,
                       PackRandomizer packRandomizer,
                       CardSkinRepository cardSkinRepository,
                       SkinService skinService,
                       CardBackService cardBackService,
                       IdempotencyService idempotencyService) {
        this.cardBackService = cardBackService;
        this.playerRepository = playerRepository;
        this.cardRepository = cardRepository;
        this.playerCardRepository = playerCardRepository;
        this.packPurchaseRepository = packPurchaseRepository;
        this.packRandomizer = packRandomizer;
        this.cardSkinRepository = cardSkinRepository;
        this.skinService = skinService;
        this.idempotencyService = idempotencyService;
    }

    /**
     * Listado de sobres disponibles con su precio y probabilidades. Los
     * sobres full-art no sortean por rareza, así que van sin probabilidades.
     */
    public List<PackInfoDTO> listPacks() {
        List<PackInfoDTO> result = new ArrayList<>();
        for (PackType type : PackType.values()) {
            Map<String, Double> probs = new java.util.LinkedHashMap<>();
            if (!type.isFullArtPack()) {
                for (RarityBucket b : RarityBucket.values()) {
                    probs.put(b.name(), type.probabilityOf(b));
                }
            }
            result.add(new PackInfoDTO(type.name(), type.getPrice(), type.getCardsPerPack(), probs));
        }
        return result;
    }

    /**
     * Buys and opens a pack of the given type for the player, idempotently.
     *
     * <p>With the same {@code Idempotency-Key}, a replay returns the same pack
     * (the same already-drawn and persisted cards) without charging again.
     *
     * @param playerId       authenticated player ID
     * @param packTypeName   pack type to open
     * @param idempotencyKey {@code Idempotency-Key} header value; null = no deduplication
     * @throws IllegalArgumentException if the player does not exist or the type is invalid
     * @throws IllegalStateException    if the player does not have enough coins
     *                                  or the catalog has no cards
     */
    public OpenedPackResponse openPack(String playerId, String packTypeName, String idempotencyKey) {
        return idempotencyService.execute(idempotencyKey, playerId, "pack:open",
                OpenedPackResponse.class, () -> doOpenPack(playerId, packTypeName));
    }

    private OpenedPackResponse doOpenPack(String playerId, String packTypeName) {
        PackType packType = parsePackType(packTypeName);
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Player not found"));

        if (player.getCoins() < packType.getPrice()) {
            throw new IllegalStateException("Insufficient coins");
        }

        if (packType.isFullArtPack()) {
            return openFullArtPack(player, packType);
        }

        List<Card> pool = getDrawablePool();
        if (pool.isEmpty()) {
            throw new IllegalStateException("Card catalog is empty");
        }

        List<Card> drawn = packRandomizer.drawCards(packType, pool);

        // Descuento monedas
        player.setCoins(player.getCoins() - packType.getPrice());

        // Agrego a colección de cantidades (suma o crea) y al set "unique" para deck-building.
        for (Card card : drawn) {
            incrementQuantity(player, card);
            if (player.getCollection().stream().noneMatch(c -> c.getId().equals(card.getId()))) {
                player.addToCollection(card);
            }
        }
        playerRepository.save(player);

        // Skin bonus
        CardSkinDTO bonusSkinDTO = null;
        String droppedSkinId = null;
        if (packRandomizer.shouldDropSkin(packType)) {
            List<CardSkin> allSkins = cardSkinRepository.findAll();
            if (!allSkins.isEmpty()) {
                CardSkin droppedSkin = allSkins.get(
                        new java.util.Random().nextInt(allSkins.size()));
                skinService.grantSkin(playerId, droppedSkin.getId(), 1, player);
                droppedSkinId = droppedSkin.getId();
                bonusSkinDTO = CardSkinDTO.builder()
                        .id(droppedSkin.getId())
                        .name(droppedSkin.getName())
                        .baseCardId(droppedSkin.getBaseCard().getId())
                        .origin(droppedSkin.getOrigin().name())
                        .imageUrl(droppedSkin.getImageUrl())
                        .imageUrlLarge(droppedSkin.getImageUrlLarge())
                        .tag(droppedSkin.getSkinTag())
                        .build();
            }
        }

        // Dorso de carta bonus: solo se sortean los de origen PACK que el
        // jugador todavía no posee (si ya tiene todos, no cae nada).
        CardBackDTO bonusCardBackDTO = null;
        if (packRandomizer.shouldDropCardBack(packType)) {
            bonusCardBackDTO = cardBackService.dropRandomPackCardBack(playerId);
        }

        // Histórico
        PackPurchase purchase = new PackPurchase();
        purchase.setId(UUID.randomUUID().toString());
        purchase.setPlayerId(playerId);
        purchase.setPackType(packType);
        purchase.setPrice(packType.getPrice());
        purchase.setCardIds(drawn.stream().map(Card::getId).collect(Collectors.joining(",")));
        purchase.setSkinId(droppedSkinId);
        purchase.setCreatedAt(LocalDateTime.now());
        packPurchaseRepository.save(purchase);

        List<CardSummaryDTO> dtos = drawn.stream().map(this::toSummary).collect(Collectors.toList());
        OpenedPackResponse response = new OpenedPackResponse(
                packType.name(), dtos, player.getCoins());
        response.setBonusSkin(bonusSkinDTO);
        response.setBonusCardBack(bonusCardBackDTO);
        return response;
    }

    /**
     * Abre un sobre full-art: entrega una única skin garantizada, sorteada
     * con preferencia entre las que el jugador todavía no posee. Si el
     * jugador no tiene la carta base de la skin, también la recibe en su
     * colección, y la skin queda equipada automáticamente.
     */
    private OpenedPackResponse openFullArtPack(Player player, PackType packType) {
        List<CardSkin> allSkins = cardSkinRepository.findAll();
        if (allSkins.isEmpty()) {
            throw new IllegalStateException("No full art skins available");
        }

        java.util.Set<String> ownedSkinIds = skinService.getPlayerSkins(player.getId()).stream()
                .map(ar.edu.utn.frc.tup.piii.dtos.skin.PlayerSkinDTO::getSkinId)
                .collect(Collectors.toSet());
        List<CardSkin> unowned = allSkins.stream()
                .filter(s -> !ownedSkinIds.contains(s.getId()))
                .collect(Collectors.toList());
        List<CardSkin> candidates = unowned.isEmpty() ? allSkins : unowned;
        CardSkin drawn = candidates.get(new java.util.Random().nextInt(candidates.size()));

        player.setCoins(player.getCoins() - packType.getPrice());

        Card baseCard = drawn.getBaseCard();
        boolean ownsBase = playerCardRepository
                .findByPlayer_IdAndCard_Id(player.getId(), baseCard.getId())
                .isPresent();
        if (!ownsBase) {
            incrementQuantity(player, baseCard);
        }
        if (player.getCollection().stream().noneMatch(c -> c.getId().equals(baseCard.getId()))) {
            player.addToCollection(baseCard);
        }
        playerRepository.save(player);

        skinService.grantSkin(player.getId(), drawn.getId(), 1, player);
        skinService.equipSkin(player.getId(), baseCard.getId(), drawn.getId());

        PackPurchase purchase = new PackPurchase();
        purchase.setId(UUID.randomUUID().toString());
        purchase.setPlayerId(player.getId());
        purchase.setPackType(packType);
        purchase.setPrice(packType.getPrice());
        purchase.setCardIds(baseCard.getId());
        purchase.setSkinId(drawn.getId());
        purchase.setCreatedAt(LocalDateTime.now());
        packPurchaseRepository.save(purchase);

        // La carta revelada se muestra con el arte de la skin, no el base.
        CardSummaryDTO summary = toSummary(baseCard);
        summary.setImageUrl(drawn.getImageUrl());
        summary.setImageUrlLarge(drawn.getImageUrlLarge());
        summary.setFullArt(true);

        OpenedPackResponse response = new OpenedPackResponse(
                packType.name(), List.of(summary), player.getCoins());
        response.setBonusSkin(CardSkinDTO.builder()
                .id(drawn.getId())
                .name(drawn.getName())
                .baseCardId(baseCard.getId())
                .origin(drawn.getOrigin().name())
                .imageUrl(drawn.getImageUrl())
                .imageUrlLarge(drawn.getImageUrlLarge())
                .tag(drawn.getSkinTag())
                .build());
        return response;
    }

    /**
     * Pool de cartas elegibles para los sobres. Excluye:
     * <ul>
     *   <li><b>Energías básicas</b> ({@code supertype == ENERGY && rarity == COMMON}):
     *       son cartas que el jugador siempre tiene disponibles, no aportan valor sortearlas.</li>
     *   <li><b>Cartas Pokesecret</b> ({@code id.startsWith("pokesecret-")}):
     *       easter eggs obtenidos por exploración del mundo, no pueden caer en sobres.</li>
     * </ul>
     */
    private List<Card> getDrawablePool() {
        return cardRepository.findAll().stream()
                .filter(c -> !isBasicEnergy(c))
                .filter(c -> !isPokesecret(c))
                .filter(c -> c.getRarity() != CardRarity.RARE_ULTRA)
                .collect(Collectors.toList());
    }

    private boolean isBasicEnergy(Card c) {
        return c.getSupertype() == Supertype.ENERGY
                && c.getRarity() == CardRarity.COMMON;
    }

    private boolean isPokesecret(Card c) {
        return c.getId() != null && c.getId().startsWith(POKESECRET_ID_PREFIX);
    }

    private void incrementQuantity(Player player, Card card) {
        PlayerCard pc = playerCardRepository
                .findByPlayer_IdAndCard_Id(player.getId(), card.getId())
                .orElseGet(() -> {
                    PlayerCard fresh = new PlayerCard();
                    fresh.setPlayer(player);
                    fresh.setCard(card);
                    fresh.setQuantity(0);
                    return fresh;
                });
        pc.setQuantity(pc.getQuantity() + 1);
        playerCardRepository.save(pc);
    }

    private PackType parsePackType(String name) {
        if (name == null) {
            throw new IllegalArgumentException("packType is required");
        }
        try {
            return PackType.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid pack type: " + name);
        }
    }

    private CardSummaryDTO toSummary(Card card) {
        CardSummaryDTO dto = new CardSummaryDTO();
        dto.setId(card.getId());
        dto.setName(card.getName());
        dto.setSupertype(card.getSupertype().name());
        dto.setImageUrl(card.getImageUrl());
        dto.setImageUrlLarge(card.getImageUrlLarge());
        dto.setRarity(card.getRarity() != null ? card.getRarity().name() : null);
        dto.setFullArt(card.isFullArt());
        return dto;
    }
}
