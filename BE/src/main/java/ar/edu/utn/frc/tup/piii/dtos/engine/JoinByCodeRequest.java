package ar.edu.utn.frc.tup.piii.dtos.engine;

import lombok.Data;

/**
 * Request body to join a match by its shareable join code.
 */
@Data
public class JoinByCodeRequest {

    /** The join code of the room to enter. */
    private String code;

    /** Identifier of the deck the joining player wants to use. */
    private String deckId;

    /** Password for private rooms (optional). */
    private String password;
}
