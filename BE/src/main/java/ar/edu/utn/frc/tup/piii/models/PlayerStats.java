package ar.edu.utn.frc.tup.piii.models;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Data;

/**
 * Persistent per-player statistics that accumulate through gameplay.
 *
 * <p>Tracked while matches are played: total/win/loss/tie counts, accumulated
 * in-match time, deck usage and card play counts, so the profile can show the
 * player's most played card and most used deck.
 */
@Data
@Entity
@Table(name = "player_stats")
public class PlayerStats {

    @Id
    private String playerId;

    @Column(nullable = false)
    private long matchesPlayed;

    @Column(nullable = false)
    private long matchesWon;

    @Column(nullable = false)
    private long matchesLost;

    @Column(nullable = false)
    private long matchesTied;

    @Column(nullable = false)
    private long totalMatchTimeMs;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "player_stats_deck_usage", joinColumns = @JoinColumn(name = "player_id"))
    @MapKeyColumn(name = "deck_id")
    @Column(name = "times_used")
    private Map<String, Long> deckUsage = new ConcurrentHashMap<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "player_stats_card_plays", joinColumns = @JoinColumn(name = "player_id"))
    @MapKeyColumn(name = "card_id")
    @Column(name = "plays")
    private Map<String, Long> cardPlays = new ConcurrentHashMap<>();

    public PlayerStats() {
    }

    public PlayerStats(String playerId) {
        this.playerId = playerId;
    }
}