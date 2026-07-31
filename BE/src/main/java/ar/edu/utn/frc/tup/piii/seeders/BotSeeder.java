package ar.edu.utn.frc.tup.piii.seeders;

import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.Deck;
import ar.edu.utn.frc.tup.piii.models.Player;
import lombok.Data;
import ar.edu.utn.frc.tup.piii.repositories.CardRepository;
import ar.edu.utn.frc.tup.piii.repositories.PlayerRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds three AI bot players (one per {@link ar.edu.utn.frc.tup.piii.engine.bot.BotDifficulty})
 * along with a starter deck each, so the lobby "Play vs Bot" feature has a
 * concrete opponent to spawn.
 *
 * <p>Runs after {@link DataSeeder} (which loads cards) thanks to {@link Order}.
 * Skips work whenever the bots already exist, so it stays idempotent across
 * application restarts.
 */
@Component
@Order(10)
public class BotSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BotSeeder.class);

    /** Bot id → deck name in {@code initial-decks.json} used as its starter deck. */
    private static final Map<String, String> BOT_DECK_MAP = Map.of(
            "bot-easy", "Chespin Deck",
            "bot-medium", "Fennekin Deck",
            "bot-hard", "Froakie Deck"
    );

    private final PlayerRepository playerRepository;
    private final CardRepository cardRepository;
    private final ObjectMapper objectMapper;

    public BotSeeder(PlayerRepository playerRepository,
                     CardRepository cardRepository,
                     ObjectMapper objectMapper) {
        this.playerRepository = playerRepository;
        this.cardRepository = cardRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<DeckSeed> deckSeeds = loadDeckSeeds();
        if (deckSeeds.isEmpty()) {
            log.warn("BotSeeder: no deck seeds found in initial-decks.json; bots not created.");
            return;
        }

        int created = 0;
        int repaired = 0;
        for (Map.Entry<String, String> entry : BOT_DECK_MAP.entrySet()) {
            String botId = entry.getKey();
            String deckName = entry.getValue();

            DeckSeed deckSeed = deckSeeds.stream()
                    .filter(d -> deckName.equalsIgnoreCase(d.getName()))
                    .findFirst()
                    .orElse(deckSeeds.get(0));

            Player existing = playerRepository.findById(botId).orElse(null);
            if (existing != null) {
                // Auto-reparación: un bot con mazo vacío/inválido (p. ej. seedeado antes
                // de que las cartas estuvieran cargadas) rompe TODA partida vs bot al
                // validar su mazo ("Deck must have exactly 60 cards (has 0)"). La vieja
                // idempotencia por existsById lo dejaba roto para siempre; ahora lo curamos.
                if (hasValidDeck(existing)) {
                    continue;
                }
                repairBotDeck(existing, deckSeed);
                playerRepository.save(existing);
                repaired++;
                continue;
            }

            Player bot = buildBotPlayer(botId, deckSeed);
            playerRepository.save(bot);
            created++;
        }

        log.info("BotSeeder: {} bot(s) creados, {} reparados.", created, repaired);
    }

    /** True cuando el bot tiene un mazo activo con exactamente 60 cartas. */
    private boolean hasValidDeck(Player bot) {
        Deck deck = findActiveDeck(bot);
        return deck != null && deck.getCards() != null && deck.getCards().size() == 60;
    }

    /**
     * Reconstruye las cartas del mazo activo del bot (y su colección) desde la semilla,
     * para que vuelva a tener 60 cartas válidas y poseídas. Reutiliza el mazo existente
     * si lo hay (mantiene su id), o crea uno nuevo si el bot quedó sin mazo.
     */
    private void repairBotDeck(Player bot, DeckSeed deckSeed) {
        List<Card> cards = expandDeckCards(deckSeed);

        if (bot.getDecks() == null) {
            bot.setDecks(new ArrayList<>());
        }
        Deck deck = findActiveDeck(bot);
        if (deck == null) {
            deck = new Deck();
            deck.setId(UUID.randomUUID().toString());
            deck.setFavorite(true);
            bot.getDecks().add(deck);
        }
        deck.setName(deckSeed.getName());
        deck.setCards(new ArrayList<>(cards));
        bot.setActiveDeckId(deck.getId());

        // El bot debe "poseer" cada carta distinta del mazo para pasar la validación.
        if (bot.getCollection() == null) {
            bot.setCollection(new ArrayList<>());
        }
        Set<String> ownedIds = new HashSet<>();
        for (Card owned : bot.getCollection()) {
            ownedIds.add(owned.getId());
        }
        for (Card card : cards) {
            if (ownedIds.add(card.getId())) {
                bot.getCollection().add(card);
            }
        }
    }

    /** Devuelve el mazo activo del bot (o el primero), o null si no tiene mazos. */
    private Deck findActiveDeck(Player bot) {
        List<Deck> decks = bot.getDecks();
        if (decks == null || decks.isEmpty()) {
            return null;
        }
        String activeId = bot.getActiveDeckId();
        if (activeId != null) {
            for (Deck deck : decks) {
                if (activeId.equals(deck.getId())) {
                    return deck;
                }
            }
        }
        return decks.get(0);
    }

    private Player buildBotPlayer(String botId, DeckSeed deckSeed) {
        Player bot = new Player();
        bot.setId(botId);
        bot.setUsername(botId);
        bot.setTrainerName(prettyName(botId));
        bot.setEmail(botId + "@bots.local");
        // Password is irrelevant for bots, but the column is non-null and would be
        // used by any code that accidentally tried to log them in.
        bot.setPassword("$2a$10$0000000000000000000000.0000000000000000000000000000000");
        bot.setDateOfBirth(LocalDate.of(2000, 1, 1));
        bot.setCoins(0);
        bot.setBot(true);
        bot.setDecks(new ArrayList<>());
        bot.setCollection(new ArrayList<>());

        Deck deck = new Deck();
        deck.setId(UUID.randomUUID().toString());
        deck.setName(deckSeed.getName());
        deck.setFavorite(true);
        List<Card> cards = expandDeckCards(deckSeed);
        deck.setCards(cards);

        // Bots also "own" each distinct card in their deck so deck validation does
        // not fail; duplicates in the collection would break the @ManyToMany join.
        Set<String> seenIds = new HashSet<>();
        for (Card card : cards) {
            if (seenIds.add(card.getId())) {
                bot.getCollection().add(card);
            }
        }
        bot.getDecks().add(deck);
        bot.setActiveDeckId(deck.getId());

        return bot;
    }

    private List<Card> expandDeckCards(DeckSeed deckSeed) {
        List<Card> cards = new ArrayList<>();
        if (deckSeed.getCards() == null) {
            return cards;
        }
        for (CardSeed cardSeed : deckSeed.getCards()) {
            List<Card> matches = cardRepository.findByName(cardSeed.getName());
            if (matches.isEmpty()) continue;
            Card template = matches.get(0);
            for (int i = 0; i < cardSeed.getCount(); i++) {
                cards.add(template);
            }
        }
        return cards;
    }

    private List<DeckSeed> loadDeckSeeds() {
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("data/initial-decks.json")) {
            if (is == null) {
                return List.of();
            }
            return objectMapper.readValue(is, new TypeReference<List<DeckSeed>>() {});
        } catch (Exception e) {
            log.warn("BotSeeder: failed to load initial-decks.json", e);
            return List.of();
        }
    }

    private String prettyName(String botId) {
        // bot-easy -> Bot Easy
        String[] parts = botId.split("-");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            sb.append(Character.toUpperCase(part.charAt(0)))
              .append(part.substring(1))
              .append(' ');
        }
        return sb.toString().trim();
    }

    @Data
    private static class DeckSeed {
        private String name;
        private List<CardSeed> cards;
    }

    @Data
    private static class CardSeed {
        private String name;
        private int count;
    }
}
