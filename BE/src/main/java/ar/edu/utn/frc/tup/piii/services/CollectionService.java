package ar.edu.utn.frc.tup.piii.services;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.utn.frc.tup.piii.dtos.player.CardSummaryDTO;
import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.CardSkin;
import ar.edu.utn.frc.tup.piii.models.Player;
import ar.edu.utn.frc.tup.piii.models.PlayerCard;
import ar.edu.utn.frc.tup.piii.repositories.CardRepository;
import ar.edu.utn.frc.tup.piii.repositories.PlayerCardRepository;
import ar.edu.utn.frc.tup.piii.repositories.PlayerRepository;

@Service
public class CollectionService {

    private final PlayerRepository playerRepository;
    private final CardRepository cardRepository;
    private final PlayerCardRepository playerCardRepository;

    public CollectionService(PlayerRepository playerRepository,
                             CardRepository cardRepository,
                             PlayerCardRepository playerCardRepository) {
        this.playerRepository = playerRepository;
        this.cardRepository = cardRepository;
        this.playerCardRepository = playerCardRepository;
    }

    public boolean cardExists(String cardId) {
        return cardRepository.existsById(cardId);
    }

    public List<CardSummaryDTO> getCollection(String playerId, int page, int size) {
        Player player = getPlayerOrThrow(playerId);
        List<Card> collection = player.getCollection();
        if (collection == null) {
            return List.of();
        }
        int start = page * size;
        if (start >= collection.size()) {
            return List.of();
        }
        int end = Math.min(start + size, collection.size());
        Map<String, PlayerCard> skinLookup = playerCardRepository
                .findByPlayer_Id(playerId).stream()
                .collect(Collectors.toMap(pc -> pc.getCard().getId(), pc -> pc, (a, b) -> a));
        return collection.subList(start, end).stream()
                .map(card -> toCardSummary(card, skinLookup.get(card.getId())))
                .collect(Collectors.toList());
    }

    /**
     * Resumen de una carta concreta resolviendo la skin activa del jugador,
     * con la misma lógica de imagen que {@link #getCollection}.
     */
    public CardSummaryDTO getCardSummary(String playerId, String cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Card not found"));
        PlayerCard playerCard = playerCardRepository
                .findByPlayer_IdAndCard_Id(playerId, cardId)
                .orElse(null);
        return toCardSummary(card, playerCard);
    }

    @Transactional
    public void addToCollection(String playerId, String cardId) {
        Player player = getPlayerOrThrow(playerId);
        if (player.getCollection() == null
                || player.getCollection().stream().noneMatch(c -> c.getId().equals(cardId))) {
            Card card = cardRepository.findById(cardId)
                    .orElseThrow(() -> new IllegalArgumentException("Card not found"));
            player.addToCollection(card);
            playerRepository.save(player);
        }
    }

    @Transactional
    public void grantAllCards(String email) {
        Player player = playerRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Player not found: " + email));
        List<Card> allCards = cardRepository.findAll();
        for (Card card : allCards) {
            if (player.getCollection() == null
                    || player.getCollection().stream().noneMatch(c -> c.getId().equals(card.getId()))) {
                player.addToCollection(card);
            }
            PlayerCard pc = playerCardRepository
                    .findByPlayer_IdAndCard_Id(player.getId(), card.getId())
                    .orElseGet(() -> {
                        PlayerCard fresh = new PlayerCard();
                        fresh.setPlayer(player);
                        fresh.setCard(card);
                        fresh.setQuantity(0);
                        return fresh;
                    });
            if (pc.getQuantity() < 1) {
                pc.setQuantity(4);
            }
            playerCardRepository.save(pc);
        }
        playerRepository.save(player);
    }

    @Transactional
    public void removeFromCollection(String playerId, String cardId) {
        Player player = getPlayerOrThrow(playerId);
        if (player.getCollection() != null) {
            player.getCollection().removeIf(c -> c.getId().equals(cardId));
            playerRepository.save(player);
        }
    }

    private Player getPlayerOrThrow(String playerId) {
        return playerRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Player not found"));
    }

    private CardSummaryDTO toCardSummary(Card card, PlayerCard playerCard) {
        CardSummaryDTO dto = new CardSummaryDTO();
        dto.setId(card.getId());
        dto.setName(card.getName());
        dto.setSupertype(card.getSupertype().name());
        dto.setRarity(card.getRarity() != null ? card.getRarity().name() : null);

        if (playerCard != null && playerCard.getActiveSkin() != null) {
            CardSkin skin = playerCard.getActiveSkin();
            dto.setImageUrl(skin.getImageUrl());
            dto.setImageUrlLarge(skin.getImageUrlLarge());
            dto.setFullArt(true);
            dto.setActiveSkinId(skin.getId());
            dto.setActiveSkinName(skin.getName());
        } else {
            dto.setImageUrl(card.getImageUrl());
            dto.setImageUrlLarge(card.getImageUrlLarge());
            dto.setFullArt(card.isFullArt());
        }
        return dto;
    }
}
