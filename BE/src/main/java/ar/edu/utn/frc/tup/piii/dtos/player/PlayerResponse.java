package ar.edu.utn.frc.tup.piii.dtos.player;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class PlayerResponse {
    private String id;
    private String username;
    private String trainerName;
    private String email;
    private LocalDate dateOfBirth;
    private int level;
    private int experience;
    private int xpForNextLevel;
    private int xpIntoCurrentLevel;
    private int coins;
    private String activeDeckId;
    /** Dorso de carta equipado; null = dorso clásico por defecto. */
    private String activeCardBackId;
    private String activeCardBackUrl;
    private String character;
    private String color;
    private List<String> collectionCardIds;
    /** Cantidad de copias por carta (cardId -> cantidad). */
    private Map<String, Integer> collectionQuantities;
    private List<DeckSummary> decks;

    @Data
    public static class DeckSummary {
        private String id;
        private String name;
        private int cardCount;
    }
}
