package ar.edu.utn.frc.tup.piii.dtos.engine;

import lombok.Data;

/**
 * Request used to select which player starts after a correct opening toss.
 */
@Data
public class ChooseFirstTurnRequest {

    private String starterPlayerId;
}
