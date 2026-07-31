package ar.edu.utn.frc.tup.piii.dtos.engine;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Request body for creating a new match. The authenticated host optionally
 * specifies which of their decks to play with.
 */
@Data
public class CreateMatchRequest {

    /** Identifier of the deck the host wants to use. */
    private String deckId;

    /** Display name of the room shown in the lobby list. */
    private String roomName;

    /** Optional description shown in the room card. */
    private String description;

    /** Whether the room is private (only joinable by code). */
    @JsonProperty("isPrivate")
    private boolean isPrivate;

    /** Whether the room appears in the public lobby list. */
    private boolean showInList = true;

    /** Optional password required to join the room. */
    private String password;

    /** Whether spectators are allowed to watch the match. */
    private boolean allowSpectators;

    /** If true, the match is auto-joined and started against a bot opponent. */
    @JsonProperty("isBotMatch")
    private boolean isBotMatch;

    /** Bot difficulty (EASY/MEDIUM/HARD). Used only when isBotMatch=true. */
    private String botDifficulty;

}
