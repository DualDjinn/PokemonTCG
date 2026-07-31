package ar.edu.utn.frc.tup.piii.dtos.engine;

import com.fasterxml.jackson.annotation.JsonProperty;
import ar.edu.utn.frc.tup.piii.models.MatchStatus;
import lombok.Data;

/**
 * Response returned after a match is created, exposing the identifier and the
 * code an opponent uses to join.
 */
@Data
public class CreateMatchResponse {

    /** Generated match identifier. */
    private String matchId;

    /** Code the host shares so an opponent can join the match. */
    private String joinCode;

    /** Current lifecycle state of the match. */
    private MatchStatus status;

    /** Display name of the room. */
    private String roomName;

    /** Optional description shown in the room card. */
    private String description;

    /** Whether the room is private. */
    @JsonProperty("isPrivate")
    private boolean isPrivate;

    /** Whether the room appears in the public lobby list. */
    private boolean showInList;

    /** Whether the room has a password set. */
    private boolean hasPassword;

    /** Whether spectators are allowed. */
    private boolean allowSpectators;

    /** Trainer name of the host. */
    private String hostTrainerName;

    /** Character id of the host. */
    private String hostCharacter;

    /** Trainer name of the guest; null while waiting. */
    private String guestTrainerName;

    /** Character id of the guest; null while waiting. */
    private String guestCharacter;

    /** Dorso de carta equipado por el host (null = por defecto). */
    private String hostCardBackUrl;

    /** Dorso de carta equipado por el guest (null = por defecto). */
    private String guestCardBackUrl;

    /** Whether the host has started the match (startMatch was called). */
    private boolean started;

    /** Whether the host is ready. */
    private boolean hostReady;

    /** Whether the guest is ready. */
    private boolean guestReady;
}
