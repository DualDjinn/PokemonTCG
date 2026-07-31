package ar.edu.utn.frc.tup.piii.seeders;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.Deck;
import ar.edu.utn.frc.tup.piii.models.Player;
import ar.edu.utn.frc.tup.piii.models.PlayerCard;
import ar.edu.utn.frc.tup.piii.repositories.CardRepository;
import ar.edu.utn.frc.tup.piii.repositories.PlayerCardRepository;
import ar.edu.utn.frc.tup.piii.repositories.PlayerRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import lombok.Data;

@Component
@Order(11)
public class DevPlayerSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevPlayerSeeder.class);
    private static final int DEV_COINS = 99999;

    private final PlayerRepository playerRepository;
    private final CardRepository cardRepository;
    private final PlayerCardRepository playerCardRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    public DevPlayerSeeder(PlayerRepository playerRepository,
                           CardRepository cardRepository,
                           PlayerCardRepository playerCardRepository,
                           PasswordEncoder passwordEncoder,
                           ObjectMapper objectMapper) {
        this.playerRepository = playerRepository;
        this.cardRepository = cardRepository;
        this.playerCardRepository = playerCardRepository;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedHernan();
        seedSantoro();
    }

    private void seedHernan() {
        if (playerRepository.findByEmail("hernanmorais@gmail.com").isPresent()) {
            log.info("DevPlayerSeeder: hernan already exists; skipping.");
            return;
        }

        Player player = new Player();
        player.setId(UUID.randomUUID().toString());
        player.setUsername("hernan");
        player.setTrainerName("hernan");
        player.setEmail("hernanmorais@gmail.com");
        player.setPassword(passwordEncoder.encode("Hernan123"));
        player.setDateOfBirth(LocalDate.of(2000, 1, 1));
        player.setCoins(DEV_COINS);
        player.setBot(false);
        player.setDecks(new ArrayList<>());
        player.setCollection(new ArrayList<>());

        List<DeckSeed> deckSeeds = loadDeckSeeds();
        for (DeckSeed deckSeed : deckSeeds) {
            Deck deck = buildDeck(deckSeed);
            player.getDecks().add(deck);
            Set<String> seenIds = new HashSet<>();
            for (Card card : deck.getCards()) {
                if (seenIds.add(card.getId())) {
                    player.getCollection().add(card);
                }
            }
            if (player.getActiveDeckId() == null) {
                player.setActiveDeckId(deck.getId());
            }
        }

        playerRepository.save(player);

        for (Deck deck : player.getDecks()) {
            for (Card card : deck.getCards()) {
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
        }

        log.info("DevPlayerSeeder: created hernan with initial decks and {} coins.", DEV_COINS);
    }

    private void seedSantoro() {
        if (playerRepository.findByEmail("santoro@gmail.com").isPresent()) {
            log.info("DevPlayerSeeder: santoro already exists; skipping.");
            return;
        }

        Player player = new Player();
        player.setId(UUID.randomUUID().toString());
        player.setUsername("santoro");
        player.setTrainerName("santoro");
        player.setEmail("santoro@gmail.com");
        player.setPassword(passwordEncoder.encode("Santoro123"));
        player.setDateOfBirth(LocalDate.of(2000, 1, 1));
        player.setCoins(DEV_COINS);
        player.setBot(false);
        player.setDecks(new ArrayList<>());
        player.setCollection(new ArrayList<>());

        List<Card> allCards = cardRepository.findAll();
        Set<String> seenIds = new HashSet<>();
        for (Card card : allCards) {
            if (seenIds.add(card.getId())) {
                player.getCollection().add(card);
            }
        }

        List<DeckSeed> deckSeeds = loadDeckSeeds();
        for (DeckSeed deckSeed : deckSeeds) {
            Deck deck = buildDeck(deckSeed);
            player.getDecks().add(deck);
            if (player.getActiveDeckId() == null) {
                player.setActiveDeckId(deck.getId());
            }
        }

        playerRepository.save(player);

        for (Card card : allCards) {
            PlayerCard pc = new PlayerCard();
            pc.setPlayer(player);
            pc.setCard(card);
            pc.setQuantity(4);
            playerCardRepository.save(pc);
        }

        log.info("DevPlayerSeeder: created santoro with ALL {} cards and {} coins.",
                allCards.size(), DEV_COINS);
    }

    private Deck buildDeck(DeckSeed deckSeed) {
        Deck deck = new Deck();
        deck.setId(UUID.randomUUID().toString());
        deck.setName(deckSeed.getName());
        List<Card> cards = new ArrayList<>();
        if (deckSeed.getCards() != null) {
            for (CardSeed cardSeed : deckSeed.getCards()) {
                List<Card> matches = cardRepository.findByName(cardSeed.getName());
                if (matches.isEmpty()) {
                    continue;
                }
                Card template = matches.get(0);
                for (int i = 0; i < cardSeed.getCount(); i++) {
                    cards.add(template);
                }
            }
        }
        deck.setCards(cards);
        return deck;
    }

    private List<DeckSeed> loadDeckSeeds() {
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("data/initial-decks.json")) {
            if (is == null) {
                return List.of();
            }
            return objectMapper.readValue(is, new TypeReference<List<DeckSeed>>() {});
        } catch (Exception e) {
            log.warn("DevPlayerSeeder: failed to load initial-decks.json", e);
            return List.of();
        }
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
