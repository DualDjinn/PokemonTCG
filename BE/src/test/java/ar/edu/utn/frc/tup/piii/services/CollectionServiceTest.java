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
import org.mockito.ArgumentCaptor;

import ar.edu.utn.frc.tup.piii.dtos.player.CardSummaryDTO;
import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.CardRarity;
import ar.edu.utn.frc.tup.piii.models.CardSkin;
import ar.edu.utn.frc.tup.piii.models.CardSkinOrigin;
import ar.edu.utn.frc.tup.piii.models.Player;
import ar.edu.utn.frc.tup.piii.models.PlayerCard;
import ar.edu.utn.frc.tup.piii.models.Supertype;
import ar.edu.utn.frc.tup.piii.repositories.CardRepository;
import ar.edu.utn.frc.tup.piii.repositories.PlayerCardRepository;
import ar.edu.utn.frc.tup.piii.repositories.PlayerRepository;

class CollectionServiceTest {

    private PlayerRepository playerRepository;
    private CardRepository cardRepository;
    private PlayerCardRepository playerCardRepository;
    private CollectionService service;

    private Player player;

    @BeforeEach
    void setUp() {
        playerRepository = mock(PlayerRepository.class);
        cardRepository = mock(CardRepository.class);
        playerCardRepository = mock(PlayerCardRepository.class);
        service = new CollectionService(playerRepository, cardRepository, playerCardRepository);

        player = new Player();
        player.setId("p1");
    }

    private Card stubCard(String id, String name, CardRarity rarity) {
        return new Card() {
            {
                try {
                    var idField = Card.class.getDeclaredField("id");
                    idField.setAccessible(true);
                    idField.set(this, id);
                    var nameField = Card.class.getDeclaredField("name");
                    nameField.setAccessible(true);
                    nameField.set(this, name);
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
            public boolean canBePlayed(ar.edu.utn.frc.tup.piii.models.GameContext ctx) {
                return false;
            }
        };
    }

    // ───────── cardExists ─────────

    @Test
    void cardExists_ExistingCard_ReturnsTrue() {
        when(cardRepository.existsById("xy1-1")).thenReturn(true);
        assertThat(service.cardExists("xy1-1")).isTrue();
    }

    @Test
    void cardExists_NonExistingCard_ReturnsFalse() {
        when(cardRepository.existsById("none")).thenReturn(false);
        assertThat(service.cardExists("none")).isFalse();
    }

    // ───────── getCollection ─────────

    @Test
    void getCollection_ReturnsPagedResults() {
        Card c1 = stubCard("xy1-1", "Venusaur", CardRarity.RARE_HOLO);
        Card c2 = stubCard("xy1-2", "Golem", CardRarity.RARE);
        Card c3 = stubCard("xy1-3", "Pikachu", CardRarity.COMMON);
        player.setCollection(new ArrayList<>(List.of(c1, c2, c3)));

        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));
        when(playerCardRepository.findByPlayer_Id("p1")).thenReturn(List.of());

        List<CardSummaryDTO> page = service.getCollection("p1", 0, 2);
        assertThat(page).hasSize(2);
        assertThat(page.get(0).getId()).isEqualTo("xy1-1");
        assertThat(page.get(1).getId()).isEqualTo("xy1-2");
    }

    @Test
    void getCollection_SecondPage_ReturnsRemaining() {
        Card c1 = stubCard("xy1-1", "Venusaur", CardRarity.RARE_HOLO);
        Card c2 = stubCard("xy1-2", "Golem", CardRarity.RARE);
        Card c3 = stubCard("xy1-3", "Pikachu", CardRarity.COMMON);
        player.setCollection(new ArrayList<>(List.of(c1, c2, c3)));

        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));
        when(playerCardRepository.findByPlayer_Id("p1")).thenReturn(List.of());

        List<CardSummaryDTO> page = service.getCollection("p1", 1, 2);
        assertThat(page).hasSize(1);
        assertThat(page.get(0).getId()).isEqualTo("xy1-3");
    }

    @Test
    void getCollection_PageBeyondSize_ReturnsEmpty() {
        Card c1 = stubCard("xy1-1", "Venusaur", CardRarity.RARE_HOLO);
        player.setCollection(new ArrayList<>(List.of(c1)));

        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));

        List<CardSummaryDTO> page = service.getCollection("p1", 5, 10);
        assertThat(page).isEmpty();
    }

    @Test
    void getCollection_NullCollection_ReturnsEmpty() {
        player.setCollection(null);
        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));

        List<CardSummaryDTO> page = service.getCollection("p1", 0, 10);
        assertThat(page).isEmpty();
    }

    @Test
    void getCollection_PlayerNotFound_Throws() {
        when(playerRepository.findById("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCollection("unknown", 0, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Player not found");
    }

    @Test
    void getCollection_WithActiveSkin_UsesSkinImage() {
        Card card = stubCard("xy1-1", "Venusaur", CardRarity.RARE_HOLO);
        player.setCollection(new ArrayList<>(List.of(card)));

        CardSkin skin = CardSkin.builder()
                .id("skin-1")
                .name("Full Art Venusaur")
                .baseCard(card)
                .origin(CardSkinOrigin.OFFICIAL)
                .imageUrl("skin-url")
                .imageUrlLarge("skin-url-large")
                .build();

        PlayerCard playerCard = new PlayerCard();
        playerCard.setCard(card);
        playerCard.setActiveSkin(skin);

        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));
        when(playerCardRepository.findByPlayer_Id("p1")).thenReturn(List.of(playerCard));

        List<CardSummaryDTO> page = service.getCollection("p1", 0, 10);
        assertThat(page).hasSize(1);
        assertThat(page.get(0).getImageUrl()).isEqualTo("skin-url");
        assertThat(page.get(0).getImageUrlLarge()).isEqualTo("skin-url-large");
        assertThat(page.get(0).isFullArt()).isTrue();
        assertThat(page.get(0).getActiveSkinId()).isEqualTo("skin-1");
        assertThat(page.get(0).getActiveSkinName()).isEqualTo("Full Art Venusaur");
    }

    @Test
    void getCollection_WithoutSkin_UsesCardImage() {
        Card card = stubCard("xy1-1", "Venusaur", CardRarity.RARE_HOLO);
        player.setCollection(new ArrayList<>(List.of(card)));

        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));
        when(playerCardRepository.findByPlayer_Id("p1")).thenReturn(List.of());

        List<CardSummaryDTO> page = service.getCollection("p1", 0, 10);
        assertThat(page).hasSize(1);
        assertThat(page.get(0).getActiveSkinId()).isNull();
    }

    // ───────── getCardSummary ─────────

    @Test
    void getCardSummary_ExistingCard_ReturnsDTO() {
        Card card = stubCard("xy1-1", "Venusaur", CardRarity.RARE_HOLO);
        when(cardRepository.findById("xy1-1")).thenReturn(Optional.of(card));
        when(playerCardRepository.findByPlayer_IdAndCard_Id("p1", "xy1-1"))
                .thenReturn(Optional.empty());

        CardSummaryDTO dto = service.getCardSummary("p1", "xy1-1");
        assertThat(dto.getId()).isEqualTo("xy1-1");
        assertThat(dto.getName()).isEqualTo("Venusaur");
        assertThat(dto.getSupertype()).isEqualTo("POKEMON");
        assertThat(dto.getRarity()).isEqualTo("RARE_HOLO");
    }

    @Test
    void getCardSummary_CardNotFound_Throws() {
        when(cardRepository.findById("none")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCardSummary("p1", "none"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Card not found");
    }

    @Test
    void getCardSummary_NullRarity_SetsNullInDTO() {
        Card card = stubCard("xy1-1", "Venusaur", null);
        when(cardRepository.findById("xy1-1")).thenReturn(Optional.of(card));
        when(playerCardRepository.findByPlayer_IdAndCard_Id("p1", "xy1-1"))
                .thenReturn(Optional.empty());

        CardSummaryDTO dto = service.getCardSummary("p1", "xy1-1");
        assertThat(dto.getRarity()).isNull();
    }

    // ───────── addToCollection ─────────

    @Test
    void addToCollection_NewCard_AddsAndSaves() {
        Card card = stubCard("xy1-5", "Charmander", CardRarity.COMMON);
        player.setCollection(new ArrayList<>());
        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));
        when(cardRepository.findById("xy1-5")).thenReturn(Optional.of(card));

        service.addToCollection("p1", "xy1-5");

        assertThat(player.getCollection()).contains(card);
        verify(playerRepository).save(player);
    }

    @Test
    void addToCollection_AlreadyOwned_DoesNotDuplicate() {
        Card card = stubCard("xy1-5", "Charmander", CardRarity.COMMON);
        player.setCollection(new ArrayList<>(List.of(card)));
        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));

        service.addToCollection("p1", "xy1-5");

        assertThat(player.getCollection()).hasSize(1);
        verify(playerRepository, never()).save(any());
    }

    @Test
    void addToCollection_NullCollection_ThrowsNpe() {
        Card card = stubCard("xy1-5", "Charmander", CardRarity.COMMON);
        player.setCollection(null);
        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));
        when(cardRepository.findById("xy1-5")).thenReturn(Optional.of(card));

        assertThatThrownBy(() -> service.addToCollection("p1", "xy1-5"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void addToCollection_CardNotFound_Throws() {
        player.setCollection(new ArrayList<>());
        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));
        when(cardRepository.findById("none")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addToCollection("p1", "none"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Card not found");
    }

    @Test
    void addToCollection_PlayerNotFound_Throws() {
        when(playerRepository.findById("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addToCollection("unknown", "xy1-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Player not found");
    }

    // ───────── removeFromCollection ─────────

    @Test
    void removeFromCollection_ExistingCard_Removes() {
        Card card = stubCard("xy1-5", "Charmander", CardRarity.COMMON);
        player.setCollection(new ArrayList<>(List.of(card)));
        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));

        service.removeFromCollection("p1", "xy1-5");

        assertThat(player.getCollection()).isEmpty();
        verify(playerRepository).save(player);
    }

    @Test
    void removeFromCollection_CardNotInCollection_NoError() {
        player.setCollection(new ArrayList<>());
        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));

        service.removeFromCollection("p1", "xy1-5");

        verify(playerRepository).save(player);
    }

    @Test
    void removeFromCollection_NullCollection_NoError() {
        player.setCollection(null);
        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));

        service.removeFromCollection("p1", "xy1-5");

        verify(playerRepository, never()).save(any());
    }

    @Test
    void removeFromCollection_PlayerNotFound_Throws() {
        when(playerRepository.findById("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.removeFromCollection("unknown", "xy1-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Player not found");
    }

    // ───────── grantAllCards ─────────

    @Test
    void grantAllCards_PlayerNotFound_Throws() {
        when(playerRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.grantAllCards("ghost@test.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Player not found: ghost@test.com");
    }

    @Test
    void grantAllCards_NewCards_AddsToCollectionAndCreatesPlayerCardsWithFourCopies() {
        player.setEmail("dev@test.com");
        player.setCollection(new ArrayList<>());
        Card c1 = stubCard("xy1-1", "Venusaur", CardRarity.RARE_HOLO);
        Card c2 = stubCard("xy1-2", "Golem", CardRarity.RARE);

        when(playerRepository.findByEmail("dev@test.com")).thenReturn(Optional.of(player));
        when(cardRepository.findAll()).thenReturn(List.of(c1, c2));
        when(playerCardRepository.findByPlayer_IdAndCard_Id("p1", "xy1-1")).thenReturn(Optional.empty());
        when(playerCardRepository.findByPlayer_IdAndCard_Id("p1", "xy1-2")).thenReturn(Optional.empty());

        service.grantAllCards("dev@test.com");

        assertThat(player.getCollection()).containsExactly(c1, c2);

        ArgumentCaptor<PlayerCard> captor = ArgumentCaptor.forClass(PlayerCard.class);
        verify(playerCardRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).allMatch(pc -> pc.getQuantity() == 4);
        verify(playerRepository).save(player);
    }

    @Test
    void grantAllCards_AlreadyOwnedWithCopies_KeepsCollectionAndQuantity() {
        Card c1 = stubCard("xy1-1", "Venusaur", CardRarity.RARE_HOLO);
        player.setEmail("dev@test.com");
        player.setCollection(new ArrayList<>(List.of(c1)));

        PlayerCard existing = new PlayerCard();
        existing.setCard(c1);
        existing.setQuantity(3);

        when(playerRepository.findByEmail("dev@test.com")).thenReturn(Optional.of(player));
        when(cardRepository.findAll()).thenReturn(List.of(c1));
        when(playerCardRepository.findByPlayer_IdAndCard_Id("p1", "xy1-1"))
                .thenReturn(Optional.of(existing));

        service.grantAllCards("dev@test.com");

        assertThat(player.getCollection()).hasSize(1); // not duplicated
        assertThat(existing.getQuantity()).isEqualTo(3); // not reset
        verify(playerCardRepository).save(existing);
        verify(playerRepository).save(player);
    }

    @Test
    void grantAllCards_NullCollection_ThrowsNpe() {
        // With a null collection, the (collection == null) branch routes into addToCollection,
        // which relies on an initialized list (same documented behaviour as addToCollection).
        player.setEmail("dev@test.com");
        player.setCollection(null);
        Card c1 = stubCard("xy1-1", "Venusaur", CardRarity.RARE_HOLO);

        when(playerRepository.findByEmail("dev@test.com")).thenReturn(Optional.of(player));
        when(cardRepository.findAll()).thenReturn(List.of(c1));

        assertThatThrownBy(() -> service.grantAllCards("dev@test.com"))
                .isInstanceOf(NullPointerException.class);
    }
}
