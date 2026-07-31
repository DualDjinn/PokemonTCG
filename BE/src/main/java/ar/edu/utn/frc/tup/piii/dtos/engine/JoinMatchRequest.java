package ar.edu.utn.frc.tup.piii.dtos.engine;

import lombok.Data;

/**
 * Request body for the second player joining an existing match.
 */
@Data
public class JoinMatchRequest {

    /** Identifier of the deck the joining player wants to use. */
    private String deckId;

    /** Password for private rooms (optional). */
    private String password;
}
