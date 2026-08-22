package ar.edu.utn.frc.tup.piii.models;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * Persistent lobby aggregate that tracks the lifecycle of a match.
 *
 * <p>This entity owns the matchmaking and setup concern (who created the match,
 * who joined, which decks were chosen and the current {@link MatchStatus}). The
 * in-memory battle state itself is owned by the engine layer and referenced here
 * through {@link #engineMatchId}, so a controller can resolve the right battle
 * from a persisted lobby record.
 */
@Data
@Entity
@Table(name = "game_match")
public class Match {

    /** Match identifier (UUID as String). */
    @Id
    private String id;

    /** Identifier of the player who created the match (host). */
    @Column(nullable = false)
    private String playerOneId;

    /** Identifier of the player who joined the match; null while WAITING. */
    private String playerTwoId;

    /** Deck chosen by player one. */
    private String deckOneId;

    /** Deck chosen by player two; set when the opponent joins. */
    private String deckTwoId;

    /** Current lifecycle state. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchStatus status;

    /** Identifier of the winning player once the match is FINISHED; otherwise null. */
    private String winnerId;

    /**
     * Identifier of the player who forfeited the match by disconnecting and
     * timing out (abandonment). Only set by the disconnect scheduler when a
     * disconnected player fails to reconnect within the allowed window.
     */
    private String forfeitPlayerId;

    /** Timestamp when the match reached the FINISHED state, otherwise null. */
    private LocalDateTime finishedAt;

    /** Creation timestamp. */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /**
     * Short, human-shareable code the opponent uses to join the match while it is
     * WAITING. Unique across active lobbies.
     */
    @Column(unique = true)
    private String joinCode;

    /**
     * Identifier of the battle aggregate inside the engine layer. Populated when
     * the opponent joins and the engine performs the opening setup. Used to bridge
     * lobby requests (state reads, action submissions) to the engine facade.
     */
    private String engineMatchId;

    /** Display name of the room shown in the lobby list. */
    @Column(name = "room_name")
    private String roomName;

    /** Optional description shown in the room card. */
    @Column(name = "description", length = 200)
    private String description;

    /** Whether the room is private (only joinable by code). */
    @JsonProperty("isPrivate")
    @Column(name = "is_private")
    private boolean isPrivate;

    /** Whether the room appears in the public lobby list. */
    @Column(name = "show_in_list")
    private boolean showInList = true;

    /** Optional password required to join the room. */
    @Column(name = "password")
    private String password;

    /** Whether spectators are allowed to watch the match. */
    @Column(name = "allow_spectators")
    private boolean allowSpectators;

    /** Timestamp of the last activity on this match (used to detect stale hosts). */
    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt;

    /** Whether the host has started the match (startMatch was called). */
    @Column(name = "started")
    private boolean started;

    /** Whether the host has marked themselves as ready. */
    @Column(name = "host_ready")
    private boolean hostReady;

    /** Whether the guest has marked themselves as ready. */
    @Column(name = "guest_ready")
    private boolean guestReady;

    /** Serialized match state JSON for persistence and reconnection. */
    @Column(name = "state_json", columnDefinition = "LONGTEXT")
    private String stateJson;

    /** Player ID who lost WebSocket connection, null if both connected. */
    @Column(name = "disconnected_player_id")
    private String disconnectedPlayerId;

    /** Timestamp when the player disconnected. */
    @Column(name = "disconnected_at")
    private LocalDateTime disconnectedAt;

    public Match() {
    }

    /**
     * Convenience constructor that creates a public, non-spectator room
     * without a display name. Delegates to the full signature.
     */
    public Match(String id, String playerOneId, String deckOneId, String joinCode) {
        this(id, playerOneId, deckOneId, joinCode, null, false, false);
    }

    public Match(String id, String playerOneId, String deckOneId, String joinCode,
                 String roomName, boolean isPrivate, boolean allowSpectators) {
        this(id, playerOneId, deckOneId, joinCode, roomName, null, isPrivate, allowSpectators);
    }

    public Match(String id, String playerOneId, String deckOneId, String joinCode,
                 String roomName, String description, boolean isPrivate, boolean allowSpectators) {
        this(id, playerOneId, deckOneId, joinCode, roomName, description, isPrivate, true, null, allowSpectators);
    }

    public Match(String id, String playerOneId, String deckOneId, String joinCode,
                 String roomName, String description, boolean isPrivate,
                 boolean showInList, String password, boolean allowSpectators) {
        this.id = id;
        this.playerOneId = playerOneId;
        this.deckOneId = deckOneId;
        this.joinCode = joinCode;
        this.roomName = roomName;
        this.description = description;
        this.isPrivate = isPrivate;
        this.showInList = showInList;
        this.password = password;
        this.allowSpectators = allowSpectators;
        this.status = MatchStatus.WAITING;
        this.createdAt = LocalDateTime.now();
        this.lastActivityAt = LocalDateTime.now();
    }

    /**
     * Indicates whether the given player belongs to this match.
     *
     * @param playerId player identifier to check
     * @return {@code true} if the player is player one or player two
     */
    public boolean containsPlayer(String playerId) {
        return playerId != null
                && (playerId.equals(playerOneId) || playerId.equals(playerTwoId));
    }
}
