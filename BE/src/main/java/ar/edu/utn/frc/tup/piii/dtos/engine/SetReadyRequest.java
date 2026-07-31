package ar.edu.utn.frc.tup.piii.dtos.engine;

import lombok.Data;

/**
 * Request body to toggle the ready state of a player in the lobby.
 */
@Data
public class SetReadyRequest {

    /** Whether the player is ready. */
    private boolean ready;
}
