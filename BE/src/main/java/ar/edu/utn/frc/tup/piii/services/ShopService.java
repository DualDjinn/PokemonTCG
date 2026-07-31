package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.shop.BuyCartItemRequest;
import ar.edu.utn.frc.tup.piii.dtos.shop.BuyCartRequest;
import ar.edu.utn.frc.tup.piii.dtos.shop.BuyCartResponse;
import ar.edu.utn.frc.tup.piii.dtos.shop.BuySplashArtsRequest;
import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.CardRarity;
import ar.edu.utn.frc.tup.piii.models.Player;
import ar.edu.utn.frc.tup.piii.models.PlayerCard;
import ar.edu.utn.frc.tup.piii.repositories.CardRepository;
import ar.edu.utn.frc.tup.piii.repositories.PlayerCardRepository;
import ar.edu.utn.frc.tup.piii.repositories.PlayerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Handles the purchase of individual cards and splash arts with the player's coins.
 *
 * <p>Cards are acquired directly by deducting coins from the player's account —
 * without going through any external payment gateway.
 *
 * <p>Both purchases are idempotent with respect to the {@code Idempotency-Key} header:
 * a double click or retry with the same key returns the cached result and does not
 * deduct coins again (see {@link IdempotencyService}).
 */
@Service
public class ShopService {

    /** Fixed price per splash art unit, aligned with the frontend (getPrice('Rare Ultra') = 4000). */
    private static final int SPLASH_ART_PRICE = 4000;

    private final PlayerRepository playerRepository;
    private final CardRepository cardRepository;
    private final PlayerCardRepository playerCardRepository;
    private final IdempotencyService idempotencyService;

    public ShopService(PlayerRepository playerRepository,
                       CardRepository cardRepository,
                       PlayerCardRepository playerCardRepository,
                       IdempotencyService idempotencyService) {
        this.playerRepository = playerRepository;
        this.cardRepository = cardRepository;
        this.playerCardRepository = playerCardRepository;
        this.idempotencyService = idempotencyService;
    }

    /**
     * Buys a set of cards by deducting the player's coins, idempotently.
     *
     * @param playerId       authenticated player ID
     * @param request        list of items to buy (cardId + quantity)
     * @param idempotencyKey {@code Idempotency-Key} header value; null = no deduplication
     * @return remaining coins after the purchase
     * @throws IllegalArgumentException if any card does not exist
     * @throws IllegalStateException    if the player does not have enough coins
     */
    public BuyCartResponse buyCards(String playerId, BuyCartRequest request, String idempotencyKey) {
        return idempotencyService.execute(idempotencyKey, playerId, "shop:buy",
                BuyCartResponse.class, () -> doBuyCards(playerId, request));
    }

    private BuyCartResponse doBuyCards(String playerId, BuyCartRequest request) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Player not found"));

        List<BuyCartItemRequest> items = request.getItems();
        if (items == null || items.isEmpty()) {
            return new BuyCartResponse(player.getCoins());
        }

        for (BuyCartItemRequest item : items) {
            int ownedQuantity = playerCardRepository
                    .findByPlayer_IdAndCard_Id(playerId, item.getCardId())
                    .map(PlayerCard::getQuantity)
                    .orElse(0);
            if (ownedQuantity + item.getQuantity() > 4) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Cannot own more than 4 copies of a card (card: " + item.getCardId() + ")");
            }
        }

        int totalCost = 0;
        for (BuyCartItemRequest item : items) {
            Card card = cardRepository.findById(item.getCardId())
                    .orElseThrow(() -> new IllegalArgumentException("Card not found: " + item.getCardId()));
            totalCost += priceFor(card.getRarity()) * item.getQuantity();
        }

        if (player.getCoins() < totalCost) {
            throw new IllegalStateException("Insufficient coins");
        }

        player.setCoins(player.getCoins() - totalCost);

        for (BuyCartItemRequest item : items) {
            Card card = cardRepository.findById(item.getCardId()).orElseThrow();
            if (player.getCollection().stream().noneMatch(c -> c.getId().equals(card.getId()))) {
                player.addToCollection(card);
            }
            PlayerCard pc = playerCardRepository
                    .findByPlayer_IdAndCard_Id(playerId, item.getCardId())
                    .orElseGet(() -> {
                        PlayerCard fresh = new PlayerCard();
                        fresh.setPlayer(player);
                        fresh.setCard(card);
                        fresh.setQuantity(0);
                        return fresh;
                    });
            pc.setQuantity(pc.getQuantity() + item.getQuantity());
            playerCardRepository.save(pc);
        }

        playerRepository.save(player);
        return new BuyCartResponse(player.getCoins());
    }

    /**
     * Buys splash arts (full-art cosmetics) by deducting the player's coins, idempotently.
     *
     * <p>Splash arts have no persisted catalog, so they are charged at a fixed price
     * per unit. The important thing is that the deduction is persisted on the player's
     * account, just like any other coin expense.
     *
     * @param playerId       authenticated player ID
     * @param request        number of splash arts to buy
     * @param idempotencyKey {@code Idempotency-Key} header value; null = no deduplication
     * @return remaining coins after the purchase
     * @throws IllegalStateException if the player does not have enough coins
     */
    public BuyCartResponse buySplashArts(String playerId, BuySplashArtsRequest request, String idempotencyKey) {
        return idempotencyService.execute(idempotencyKey, playerId, "shop:splash",
                BuyCartResponse.class, () -> doBuySplashArts(playerId, request));
    }

    private BuyCartResponse doBuySplashArts(String playerId, BuySplashArtsRequest request) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Player not found"));

        int quantity = request == null ? 0 : request.getQuantity();
        if (quantity <= 0) {
            return new BuyCartResponse(player.getCoins());
        }

        int totalCost = quantity * SPLASH_ART_PRICE;
        if (player.getCoins() < totalCost) {
            throw new IllegalStateException("Insufficient coins");
        }

        player.setCoins(player.getCoins() - totalCost);
        playerRepository.save(player);
        return new BuyCartResponse(player.getCoins());
    }

    /**
     * Coin price of a card based on its rarity — identical to the frontend (cart.model.ts).
     */
    private int priceFor(CardRarity rarity) {
        if (rarity == null) {
            return 100;
        }
        return switch (rarity) {
            case COMMON -> 50;
            case UNCOMMON -> 150;
            case RARE -> 300;
            case RARE_HOLO -> 500;
            case RARE_HOLO_EX -> 1200;
            case ACE_SPEC_RARE -> 800;
            case RARE_ULTRA -> 800;
        };
    }
}
