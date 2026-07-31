package ar.edu.utn.frc.tup.piii.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import ar.edu.utn.frc.tup.piii.dtos.shop.BuyCartItemRequest;
import ar.edu.utn.frc.tup.piii.dtos.shop.BuyCartRequest;
import ar.edu.utn.frc.tup.piii.dtos.shop.BuyCartResponse;
import ar.edu.utn.frc.tup.piii.dtos.shop.BuySplashArtsRequest;
import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.CardRarity;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.Player;
import ar.edu.utn.frc.tup.piii.models.PlayerCard;
import ar.edu.utn.frc.tup.piii.models.Supertype;
import ar.edu.utn.frc.tup.piii.repositories.CardRepository;
import ar.edu.utn.frc.tup.piii.repositories.IdempotencyRecordRepository;
import ar.edu.utn.frc.tup.piii.repositories.PlayerCardRepository;
import ar.edu.utn.frc.tup.piii.repositories.PlayerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

class ShopServiceTest {

    private PlayerRepository playerRepository;
    private CardRepository cardRepository;
    private PlayerCardRepository playerCardRepository;
    private ShopService service;

    private Player player;

    @BeforeEach
    void setUp() {
        playerRepository = mock(PlayerRepository.class);
        cardRepository = mock(CardRepository.class);
        playerCardRepository = mock(PlayerCardRepository.class);
        service = new ShopService(playerRepository, cardRepository, playerCardRepository,
                new IdempotencyService(mock(IdempotencyRecordRepository.class), new ObjectMapper()));

        player = new Player();
        player.setId("p1");
        player.setCoins(5000);
        player.setCollection(new ArrayList<>());
    }

    private Card stubCard(String id, CardRarity rarity) {
        return new Card() {
            {
                try {
                    var idField = Card.class.getDeclaredField("id");
                    idField.setAccessible(true);
                    idField.set(this, id);
                    this.setRarity(rarity);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public Supertype getSupertype() {
                return Supertype.POKEMON;
            }

            @Override
            public boolean canBePlayed(GameContext ctx) {
                return false;
            }
        };
    }

    // ───────── buyCards — happy paths ─────────

    @Test
    void buyCards_SingleCommonCard_Deducts50Coins() {
        Card card = stubCard("xy1-1", CardRarity.COMMON);
        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));
        when(cardRepository.findById("xy1-1")).thenReturn(Optional.of(card));

        BuyCartRequest request = new BuyCartRequest(
                List.of(new BuyCartItemRequest("xy1-1", 1)));

        BuyCartResponse response = service.buyCards("p1", request, null);

        assertThat(response.getRemainingCoins()).isEqualTo(4950);
        assertThat(player.getCollection()).contains(card);
        verify(playerRepository).save(player);
    }

    @Test
    void buyCards_MultipleQuantity_MultipliesPrice() {
        Card card = stubCard("xy1-1", CardRarity.COMMON);
        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));
        when(cardRepository.findById("xy1-1")).thenReturn(Optional.of(card));

        BuyCartRequest request = new BuyCartRequest(
                List.of(new BuyCartItemRequest("xy1-1", 3)));

        BuyCartResponse response = service.buyCards("p1", request, null);

        assertThat(response.getRemainingCoins()).isEqualTo(4850); // 5000 - 150
    }

    @Test
    void buyCards_MultipleItems_DeductsTotalCost() {
        Card common = stubCard("xy1-1", CardRarity.COMMON);
        Card rare = stubCard("xy1-2", CardRarity.RARE);
        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));
        when(cardRepository.findById("xy1-1")).thenReturn(Optional.of(common));
        when(cardRepository.findById("xy1-2")).thenReturn(Optional.of(rare));

        BuyCartRequest request = new BuyCartRequest(List.of(
                new BuyCartItemRequest("xy1-1", 1),
                new BuyCartItemRequest("xy1-2", 1)));

        BuyCartResponse response = service.buyCards("p1", request, null);

        assertThat(response.getRemainingCoins()).isEqualTo(4650); // 5000 - 50 - 300
    }

    // ───────── rarity pricing ─────────

    @Test
    void buyCards_UncommonCard_Costs150() {
        Card card = stubCard("xy1-1", CardRarity.UNCOMMON);
        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));
        when(cardRepository.findById("xy1-1")).thenReturn(Optional.of(card));

        BuyCartResponse response = service.buyCards("p1",
                new BuyCartRequest(List.of(new BuyCartItemRequest("xy1-1", 1))), null);

        assertThat(response.getRemainingCoins()).isEqualTo(4850);
    }

    @Test
    void buyCards_RareHoloCard_Costs500() {
        Card card = stubCard("xy1-1", CardRarity.RARE_HOLO);
        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));
        when(cardRepository.findById("xy1-1")).thenReturn(Optional.of(card));

        BuyCartResponse response = service.buyCards("p1",
                new BuyCartRequest(List.of(new BuyCartItemRequest("xy1-1", 1))), null);

        assertThat(response.getRemainingCoins()).isEqualTo(4500);
    }

    @Test
    void buyCards_RareHoloExCard_Costs1200() {
        Card card = stubCard("xy1-1", CardRarity.RARE_HOLO_EX);
        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));
        when(cardRepository.findById("xy1-1")).thenReturn(Optional.of(card));

        BuyCartResponse response = service.buyCards("p1",
                new BuyCartRequest(List.of(new BuyCartItemRequest("xy1-1", 1))), null);

        assertThat(response.getRemainingCoins()).isEqualTo(3800);
    }

    @Test
    void buyCards_AceSpecRareCard_Costs800() {
        Card card = stubCard("xy1-1", CardRarity.ACE_SPEC_RARE);
        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));
        when(cardRepository.findById("xy1-1")).thenReturn(Optional.of(card));

        BuyCartResponse response = service.buyCards("p1",
                new BuyCartRequest(List.of(new BuyCartItemRequest("xy1-1", 1))), null);

        assertThat(response.getRemainingCoins()).isEqualTo(4200);
    }

    @Test
    void buyCards_RareUltraCard_Costs800() {
        Card card = stubCard("xy1-1", CardRarity.RARE_ULTRA);
        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));
        when(cardRepository.findById("xy1-1")).thenReturn(Optional.of(card));

        BuyCartResponse response = service.buyCards("p1",
                new BuyCartRequest(List.of(new BuyCartItemRequest("xy1-1", 1))), null);

        assertThat(response.getRemainingCoins()).isEqualTo(4200);
    }

    @Test
    void buyCards_NullRarity_DefaultsTo100() {
        Card card = stubCard("xy1-1", null);
        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));
        when(cardRepository.findById("xy1-1")).thenReturn(Optional.of(card));

        BuyCartResponse response = service.buyCards("p1",
                new BuyCartRequest(List.of(new BuyCartItemRequest("xy1-1", 1))), null);

        assertThat(response.getRemainingCoins()).isEqualTo(4900);
    }

    // ───────── edge cases ─────────

    @Test
    void buyCards_EmptyItems_ReturnsCurrentCoins() {
        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));

        BuyCartResponse response = service.buyCards("p1",
                new BuyCartRequest(List.of()), null);

        assertThat(response.getRemainingCoins()).isEqualTo(5000);
        verify(playerRepository, never()).save(any());
    }

    @Test
    void buyCards_NullItems_ReturnsCurrentCoins() {
        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));

        BuyCartResponse response = service.buyCards("p1",
                new BuyCartRequest(null), null);

        assertThat(response.getRemainingCoins()).isEqualTo(5000);
    }

    @Test
    void buyCards_InsufficientCoins_Throws() {
        player.setCoins(10);
        Card card = stubCard("xy1-1", CardRarity.RARE_HOLO_EX);
        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));
        when(cardRepository.findById("xy1-1")).thenReturn(Optional.of(card));

        assertThatThrownBy(() -> service.buyCards("p1",
                new BuyCartRequest(List.of(new BuyCartItemRequest("xy1-1", 1))), null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient coins");

        verify(playerRepository, never()).save(any());
    }

    @Test
    void buyCards_CardNotFound_Throws() {
        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));
        when(cardRepository.findById("none")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buyCards("p1",
                new BuyCartRequest(List.of(new BuyCartItemRequest("none", 1))), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Card not found");
    }

    @Test
    void buyCards_PlayerNotFound_Throws() {
        when(playerRepository.findById("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buyCards("unknown",
                new BuyCartRequest(List.of(new BuyCartItemRequest("xy1-1", 1))), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Player not found");
    }

    @Test
    void buyCards_AlreadyOwnedCard_DoesNotDuplicateInCollection() {
        Card card = stubCard("xy1-1", CardRarity.COMMON);
        player.setCollection(new ArrayList<>(List.of(card)));
        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));
        when(cardRepository.findById("xy1-1")).thenReturn(Optional.of(card));

        service.buyCards("p1",
                new BuyCartRequest(List.of(new BuyCartItemRequest("xy1-1", 1))), null);

        assertThat(player.getCollection()).hasSize(1);
    }

    @Test
    void buyCards_ExactCoins_Succeeds() {
        player.setCoins(50);
        Card card = stubCard("xy1-1", CardRarity.COMMON);
        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));
        when(cardRepository.findById("xy1-1")).thenReturn(Optional.of(card));

        BuyCartResponse response = service.buyCards("p1",
                new BuyCartRequest(List.of(new BuyCartItemRequest("xy1-1", 1))), null);

        assertThat(response.getRemainingCoins()).isEqualTo(0);
    }

    @Test
    void buyCards_WithIdempotencyKey_RunsAndDeducts() {
        Card card = stubCard("xy1-1", CardRarity.COMMON);
        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));
        when(cardRepository.findById("xy1-1")).thenReturn(Optional.of(card));

        BuyCartResponse response = service.buyCards("p1",
                new BuyCartRequest(List.of(new BuyCartItemRequest("xy1-1", 1))), "key-123");

        assertThat(response.getRemainingCoins()).isEqualTo(4950);
        verify(playerRepository).save(player);
    }

    // ───────── buySplashArts ─────────

    @Test
    void buySplashArts_OneUnit_Deducts4000Coins() {
        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));

        BuyCartResponse response = service.buySplashArts("p1",
                new BuySplashArtsRequest(1), null);

        assertThat(response.getRemainingCoins()).isEqualTo(1000); // 5000 - 4000
        verify(playerRepository).save(player);
    }

    @Test
    void buySplashArts_ExactCoins_Succeeds() {
        player.setCoins(8000);
        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));

        BuyCartResponse response = service.buySplashArts("p1",
                new BuySplashArtsRequest(2), null);

        assertThat(response.getRemainingCoins()).isEqualTo(0);
    }

    @Test
    void buySplashArts_ZeroQuantity_ReturnsCurrentCoins() {
        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));

        BuyCartResponse response = service.buySplashArts("p1",
                new BuySplashArtsRequest(0), null);

        assertThat(response.getRemainingCoins()).isEqualTo(5000);
        verify(playerRepository, never()).save(any());
    }

    @Test
    void buySplashArts_NegativeQuantity_ReturnsCurrentCoins() {
        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));

        BuyCartResponse response = service.buySplashArts("p1",
                new BuySplashArtsRequest(-5), null);

        assertThat(response.getRemainingCoins()).isEqualTo(5000);
        verify(playerRepository, never()).save(any());
    }

    @Test
    void buySplashArts_NullRequest_ReturnsCurrentCoins() {
        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));

        BuyCartResponse response = service.buySplashArts("p1", null, null);

        assertThat(response.getRemainingCoins()).isEqualTo(5000);
        verify(playerRepository, never()).save(any());
    }

    @Test
    void buySplashArts_InsufficientCoins_Throws() {
        player.setCoins(50);
        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));

        assertThatThrownBy(() -> service.buySplashArts("p1",
                new BuySplashArtsRequest(1), null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient coins");

        verify(playerRepository, never()).save(any());
    }

    @Test
    void buySplashArts_PlayerNotFound_Throws() {
        when(playerRepository.findById("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buySplashArts("ghost",
                new BuySplashArtsRequest(1), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Player not found");
    }

    // ───────── 4-copy limit ─────────

    @Test
    void buyCards_ExceedsFourCopyLimit_ThrowsConflict() {
        Card card = stubCard("xy1-1", CardRarity.COMMON);
        PlayerCard owned = new PlayerCard();
        owned.setQuantity(3);
        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));
        when(cardRepository.findById("xy1-1")).thenReturn(Optional.of(card));
        when(playerCardRepository.findByPlayer_IdAndCard_Id("p1", "xy1-1"))
                .thenReturn(Optional.of(owned));

        assertThatThrownBy(() -> service.buyCards("p1",
                new BuyCartRequest(List.of(new BuyCartItemRequest("xy1-1", 2))), null))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void buyCards_ExactlyFourCopies_Succeeds() {
        Card card = stubCard("xy1-1", CardRarity.COMMON);
        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));
        when(cardRepository.findById("xy1-1")).thenReturn(Optional.of(card));
        when(playerCardRepository.findByPlayer_IdAndCard_Id("p1", "xy1-1"))
                .thenReturn(Optional.empty());

        BuyCartResponse response = service.buyCards("p1",
                new BuyCartRequest(List.of(new BuyCartItemRequest("xy1-1", 4))), null);

        assertThat(response.getRemainingCoins()).isEqualTo(4800); // 5000 - 4*50
    }

    @Test
    void buyCards_IncrementsExistingPlayerCardQuantity() {
        Card card = stubCard("xy1-1", CardRarity.COMMON);
        PlayerCard owned = new PlayerCard();
        owned.setQuantity(1);
        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));
        when(cardRepository.findById("xy1-1")).thenReturn(Optional.of(card));
        when(playerCardRepository.findByPlayer_IdAndCard_Id("p1", "xy1-1"))
                .thenReturn(Optional.of(owned));

        service.buyCards("p1",
                new BuyCartRequest(List.of(new BuyCartItemRequest("xy1-1", 2))), null);

        assertThat(owned.getQuantity()).isEqualTo(3);
    }
}
