package ar.edu.utn.frc.tup.piii.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.utn.frc.tup.piii.dtos.player.CardSummaryDTO;
import ar.edu.utn.frc.tup.piii.dtos.player.DeckDTO;
import ar.edu.utn.frc.tup.piii.dtos.player.DeckValidationResult;
import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.Deck;
import ar.edu.utn.frc.tup.piii.models.Player;
import ar.edu.utn.frc.tup.piii.repositories.CardRepository;
import ar.edu.utn.frc.tup.piii.repositories.PlayerRepository;

@Service
public class DeckService {

    private final PlayerRepository playerRepository;
    private final CardRepository cardRepository;

    public DeckService(PlayerRepository playerRepository,
                       CardRepository cardRepository) {
        this.playerRepository = playerRepository;
        this.cardRepository = cardRepository;
    }

    public List<DeckDTO> getPlayerDecks(String playerId) {
        Player player = getPlayerOrThrow(playerId);
        List<DeckDTO> result = new ArrayList<>();
        if (player.getDecks() != null) {
            for (Deck deck : player.getDecks()) {
                result.add(toDeckDTO(deck, player));
            }
        }
        return result;
    }

    public DeckDTO getDeck(String playerId, String deckId) {
        Player player = getPlayerOrThrow(playerId);
        Deck deck = findDeckOrThrow(player, deckId);
        return toDeckDTO(deck, player);
    }

    private static final String POKESECRET_SET_CODE = "pokesecret";

    @Transactional
    public DeckDTO updateDeckCards(String playerId, String deckId, List<String> cardIds) {
        Player player = getPlayerOrThrow(playerId);
        Deck deck = findDeckOrThrow(player, deckId);

        List<Card> newCards = new ArrayList<>();
        if (cardIds != null) {
            for (String cardId : cardIds) {
                Card card = cardRepository.findById(cardId)
                        .orElseThrow(() -> new IllegalArgumentException("Card not found: " + cardId));
                if (POKESECRET_SET_CODE.equals(card.getSetCode())) {
                    throw new IllegalArgumentException(
                            "Card '" + card.getName() + "' is a collectible-only card and cannot be added to a deck");
                }
                newCards.add(card);
            }
        }
        deck.setCards(newCards);

        if (deckId.equals(player.getActiveDeckId()) && !deck.isValid(player, Set.of())) {
            player.setActiveDeckId(null);
        }

        playerRepository.save(player);

        // La spec indica que PUT no corre reglas de mazo: se devuelve el DTO sin valid/errors.
        return toDeckDTONoValidation(deck);
    }

    @Transactional
    public DeckDTO createDeck(String playerId, String name) {
        Player player = getPlayerOrThrow(playerId);
        Deck deck = new Deck(UUID.randomUUID().toString(), name, new ArrayList<>());
        player.addDeck(deck);
        playerRepository.save(player);
        return toDeckDTO(deck, player);
    }

    @Transactional
    public void deleteDeck(String playerId, String deckId) {
        Player player = getPlayerOrThrow(playerId);
        Deck deck = findDeckOrThrow(player, deckId);
        player.getDecks().remove(deck);
        playerRepository.save(player);
    }

    @Transactional
    public DeckDTO renameDeck(String playerId, String deckId, String newName) {
        Player player = getPlayerOrThrow(playerId);
        Deck deck = findDeckOrThrow(player, deckId);
        deck.setName(newName);
        playerRepository.save(player);
        return toDeckDTO(deck, player);
    }

    @Transactional
    public DeckDTO toggleDeckFavorite(String playerId, String deckId) {
        Player player = getPlayerOrThrow(playerId);
        Deck deck = findDeckOrThrow(player, deckId);
        deck.setFavorite(!deck.isFavorite());
        playerRepository.save(player);
        return toDeckDTO(deck, player);
    }

    public DeckValidationResult validateDeck(String playerId, String deckId) {
        Player player = getPlayerOrThrow(playerId);
        Deck deck = findDeckOrThrow(player, deckId);
        List<String> errors = deck.validate(player, Set.of());
        DeckValidationResult result = new DeckValidationResult();
        result.setValid(errors.isEmpty());
        result.setErrors(errors);
        return result;
    }

    private Player getPlayerOrThrow(String playerId) {
        return playerRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Player not found"));
    }

    private Deck findDeckOrThrow(Player player, String deckId) {
        return player.getDecks().stream()
                .filter(d -> d.getId().equals(deckId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Deck not found"));
    }

    private DeckDTO toDeckDTO(Deck deck, Player player) {
        DeckDTO dto = new DeckDTO();
        dto.setId(deck.getId());
        dto.setName(deck.getName());
        dto.setFavorite(deck.isFavorite());

        List<CardSummaryDTO> cardSummaries = new ArrayList<>();
        if (deck.getCards() != null) {
            for (Card card : deck.getCards()) {
                cardSummaries.add(toCardSummary(card));
            }
        }
        dto.setCards(cardSummaries);
        dto.setCardCount(cardSummaries.size());

        List<String> errors = deck.validate(player, Set.of());
        dto.setValid(errors.isEmpty());
        dto.setErrors(errors);

        return dto;
    }

    // Convierte Deck a DTO sin correr las reglas de validacion del mazo (para PUT).
    private DeckDTO toDeckDTONoValidation(Deck deck) {
        DeckDTO dto = new DeckDTO();
        dto.setId(deck.getId());
        dto.setName(deck.getName());
        dto.setFavorite(deck.isFavorite());

        List<CardSummaryDTO> cardSummaries = new ArrayList<>();
        if (deck.getCards() != null) {
            for (Card card : deck.getCards()) {
                cardSummaries.add(toCardSummary(card));
            }
        }
        dto.setCards(cardSummaries);
        dto.setCardCount(cardSummaries.size());
        return dto;
    }

    private CardSummaryDTO toCardSummary(Card card) {
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
