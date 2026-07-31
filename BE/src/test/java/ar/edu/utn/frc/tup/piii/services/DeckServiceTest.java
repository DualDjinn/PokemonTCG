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

import ar.edu.utn.frc.tup.piii.dtos.player.DeckDTO;
import ar.edu.utn.frc.tup.piii.dtos.player.DeckValidationResult;
import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.Deck;
import ar.edu.utn.frc.tup.piii.models.EnergyCard;
import ar.edu.utn.frc.tup.piii.models.EnergyType;
import ar.edu.utn.frc.tup.piii.models.Player;
import ar.edu.utn.frc.tup.piii.models.PokemonCard;
import ar.edu.utn.frc.tup.piii.models.PokemonStage;
import ar.edu.utn.frc.tup.piii.repositories.CardRepository;
import ar.edu.utn.frc.tup.piii.repositories.PlayerRepository;

class DeckServiceTest {

    private PlayerRepository playerRepository;
    private CardRepository cardRepository;
    private DeckService deckService;

    @BeforeEach
    void setUp() {
        playerRepository = mock(PlayerRepository.class);
        cardRepository = mock(CardRepository.class);
        deckService = new DeckService(playerRepository, cardRepository);
    }

    // ───────── getPlayerDecks ─────────

    @Test
    void getPlayerDecks_ReturnsListOfDeckDTOs() {
        Player player = playerWithDecks();
        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));

        List<DeckDTO> result = deckService.getPlayerDecks("p1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("deck-1");
        assertThat(result.get(0).getName()).isEqualTo("My Deck");
    }

    @Test
    void getPlayerDecks_NullDecks_ReturnsEmptyList() {
        Player player = new Player();
        player.setId("p1");
        player.setDecks(null);
        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));

        List<DeckDTO> result = deckService.getPlayerDecks("p1");

        assertThat(result).isEmpty();
    }

    @Test
    void getPlayerDecks_PlayerNotFound_ThrowsIllegalArg() {
        when(playerRepository.findById("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deckService.getPlayerDecks("ghost"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Player not found");
    }

    // ───────── getDeck ─────────

    @Test
    void getDeck_Success_ReturnsDeckDTO() {
        Player player = playerWithDecks();
        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));

        DeckDTO result = deckService.getDeck("p1", "deck-1");

        assertThat(result.getId()).isEqualTo("deck-1");
        assertThat(result.getCards()).hasSize(2);
    }

    @Test
    void getDeck_NotFound_ThrowsIllegalArg() {
        Player player = playerWithDecks();
        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));

        assertThatThrownBy(() -> deckService.getDeck("p1", "unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Deck not found");
    }

    // ───────── createDeck ─────────

    @Test
    void createDeck_Success() {
        Player player = new Player();
        player.setId("p1");
        player.setDecks(new ArrayList<>());
        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));

        DeckDTO result = deckService.createDeck("p1", "New Deck");

        assertThat(result.getName()).isEqualTo("New Deck");
        assertThat(result.getId()).isNotBlank();
        verify(playerRepository).save(player);
    }

    // ───────── updateDeckCards ─────────

    @Test
    void updateDeckCards_Success() {
        Player player = playerWithDecks();
        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));
        Card c = energyCard("e1");
        when(cardRepository.findById("e1")).thenReturn(Optional.of(c));

        DeckDTO result = deckService.updateDeckCards("p1", "deck-1", List.of("e1"));

        assertThat(result.getCards()).hasSize(1);
        verify(playerRepository).save(player);
    }

    @Test
    void updateDeckCards_NullCardIds_ClearsDeck() {
        Player player = playerWithDecks();
        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));

        DeckDTO result = deckService.updateDeckCards("p1", "deck-1", null);

        assertThat(result.getCards()).isEmpty();
        verify(playerRepository).save(player);
    }

    @Test
    void updateDeckCards_CardNotFound_ThrowsIllegalArg() {
        Player player = playerWithDecks();
        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));
        when(cardRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deckService.updateDeckCards("p1", "deck-1", List.of("nonexistent")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Card not found");
        verify(playerRepository, never()).save(any());
    }

    // ───────── deleteDeck ─────────

    @Test
    void deleteDeck_Success() {
        Player player = playerWithDecks();
        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));

        deckService.deleteDeck("p1", "deck-1");

        assertThat(player.getDecks()).isEmpty();
        verify(playerRepository).save(player);
    }

    // ───────── renameDeck ─────────

    @Test
    void renameDeck_Success() {
        Player player = playerWithDecks();
        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));

        DeckDTO result = deckService.renameDeck("p1", "deck-1", "Renamed");

        assertThat(result.getName()).isEqualTo("Renamed");
        verify(playerRepository).save(player);
    }

    // ───────── toggleDeckFavorite ─────────

    @Test
    void toggleDeckFavorite_TogglesFlag() {
        Player player = playerWithDecks();
        player.getDecks().get(0).setFavorite(false);
        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));

        DeckDTO result = deckService.toggleDeckFavorite("p1", "deck-1");

        assertThat(result.isFavorite()).isTrue();
        verify(playerRepository).save(player);
    }

    // ───────── validateDeck ─────────

    @Test
    void validateDeck_Valid_ReturnsNoErrors() {
        Player player = playerWithDecks();
        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));

        DeckValidationResult result = deckService.validateDeck("p1", "deck-1");

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).isNotEmpty();
    }

    // ───────── helpers ─────────

    private Player playerWithDecks() {
        Player player = new Player();
        player.setId("p1");
        List<Card> cards = new ArrayList<>();
        cards.add(basic("c1", 1));
        cards.add(basic("c2", 2));
        Deck deck = new Deck("deck-1", "My Deck", cards);
        player.setDecks(new ArrayList<>(List.of(deck)));
        return player;
    }

    private PokemonCard basic(String id, int slot) {
        return PokemonCard.builder()
                .id(id)
                .name("Basic " + slot)
                .hp(60)
                .stage(PokemonStage.BASIC)
                .pokemonType(EnergyType.COLORLESS)
                .attacks(List.of())
                .abilities(List.of())
                .weaknesses(List.of())
                .resistances(List.of())
                .costToAttack(List.of())
                .retreatCost(0)
                .build();
    }

    private EnergyCard energyCard(String id) {
        return EnergyCard.builder()
                .id(id)
                .name("Energy " + id)
                .build();
    }
}
