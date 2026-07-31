package ar.edu.utn.frc.tup.piii.seeders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.DefaultApplicationArguments;

import com.fasterxml.jackson.databind.ObjectMapper;

import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.Deck;
import ar.edu.utn.frc.tup.piii.models.Player;
import ar.edu.utn.frc.tup.piii.repositories.CardRepository;
import ar.edu.utn.frc.tup.piii.repositories.PlayerRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BotSeederTest {

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private CardRepository cardRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private BotSeeder seeder;

    @BeforeEach
    void setUp() {
        seeder = new BotSeeder(playerRepository, cardRepository, objectMapper);
    }

    @Test
    void run_AllBotsExistWithValidDecks_SkipsAll() throws Exception {
        lenient().when(cardRepository.findByName(anyString())).thenReturn(List.of());
        Deck validDeck = valid60CardDeck();
        for (String botId : List.of("bot-easy", "bot-medium", "bot-hard")) {
            Player bot = buildBotWithDeck(botId, validDeck);
            when(playerRepository.findById(botId)).thenReturn(Optional.of(bot));
        }

        seeder.run(new DefaultApplicationArguments());

        verify(playerRepository, never()).save(org.mockito.Mockito.any());
    }

    @Test
    void run_BotsDontExist_CreatesThem() throws Exception {
        when(playerRepository.findById("bot-easy")).thenReturn(Optional.empty());
        when(playerRepository.findById("bot-medium")).thenReturn(Optional.empty());
        when(playerRepository.findById("bot-hard")).thenReturn(Optional.empty());
        Card c = org.mockito.Mockito.mock(Card.class);
        when(c.getId()).thenReturn("c1");
        lenient().when(cardRepository.findByName(anyString())).thenReturn(List.of(c));

        ArgumentCaptor<Player> captor = ArgumentCaptor.forClass(Player.class);
        seeder.run(new DefaultApplicationArguments());
        verify(playerRepository, org.mockito.Mockito.times(3)).save(captor.capture());

        List<Player> bots = captor.getAllValues();
        assertThat(bots).hasSize(3);
        for (Player bot : bots) {
            assertThat(bot.isBot()).isTrue();
            assertThat(bot.getDecks()).isNotEmpty();
        }
    }

    @Test
    void run_BotHasInvalidDeck_RepairsIt() throws Exception {
        Deck invalidDeck = new Deck();
        invalidDeck.setId("deck-1");
        invalidDeck.setCards(new ArrayList<>());

        Player botEasy = buildBotWithDeck("bot-easy", invalidDeck);
        when(playerRepository.findById("bot-easy")).thenReturn(Optional.of(botEasy));
        when(playerRepository.findById("bot-medium")).thenReturn(Optional.empty());
        when(playerRepository.findById("bot-hard")).thenReturn(Optional.empty());
        Card c = org.mockito.Mockito.mock(Card.class);
        when(c.getId()).thenReturn("c1");
        when(cardRepository.findByName(anyString())).thenReturn(List.of(c));

        seeder.run(new DefaultApplicationArguments());

        verify(playerRepository).save(botEasy);
    }

    @Test
    void run_BotHasNullDeck_RepairsIt() throws Exception {
        Player botEasy = new Player();
        botEasy.setId("bot-easy");
        botEasy.setDecks(null);
        when(playerRepository.findById("bot-easy")).thenReturn(Optional.of(botEasy));
        when(playerRepository.findById("bot-medium")).thenReturn(Optional.empty());
        when(playerRepository.findById("bot-hard")).thenReturn(Optional.empty());

        seeder.run(new DefaultApplicationArguments());

        verify(playerRepository).save(botEasy);
        assertThat(botEasy.getDecks()).isNotEmpty();
    }

    @Test
    void run_CreatesBotPlayers() throws Exception {
        when(playerRepository.findById("bot-easy")).thenReturn(Optional.empty());
        when(playerRepository.findById("bot-medium")).thenReturn(Optional.empty());
        when(playerRepository.findById("bot-hard")).thenReturn(Optional.empty());
        Card c = org.mockito.Mockito.mock(Card.class);
        when(c.getId()).thenReturn("c1");
        lenient().when(cardRepository.findByName(anyString())).thenReturn(List.of(c));

        seeder.run(new DefaultApplicationArguments());

        verify(playerRepository, org.mockito.Mockito.times(3)).save(org.mockito.Mockito.any());
    }

    private Deck valid60CardDeck() {
        Deck deck = new Deck();
        deck.setId("valid-deck");
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            Card c = org.mockito.Mockito.mock(Card.class);
            when(c.getId()).thenReturn("c" + i);
            cards.add(c);
        }
        deck.setCards(cards);
        return deck;
    }

    private Player buildBotWithDeck(String botId, Deck deck) {
        Player bot = new Player();
        bot.setId(botId);
        bot.setDecks(new ArrayList<>(List.of(deck)));
        bot.setActiveDeckId(deck.getId());
        bot.setCollection(new ArrayList<>());
        for (Card c : deck.getCards()) {
            bot.getCollection().add(c);
        }
        return bot;
    }
}
