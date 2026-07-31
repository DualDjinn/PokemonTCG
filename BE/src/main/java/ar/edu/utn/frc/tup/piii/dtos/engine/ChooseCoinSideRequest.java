package ar.edu.utn.frc.tup.piii.dtos.engine;

import lombok.Data;

/**
 * Request used to submit the opening coin-side choice.
 */
@Data
public class ChooseCoinSideRequest {

    private String coinSide;
}
