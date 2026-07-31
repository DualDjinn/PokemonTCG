package ar.edu.utn.frc.tup.piii.dtos.engine;

import java.util.List;
import lombok.Data;

/**
 * Wire model for {@code PendingPlayerChoice}.
 *
 * <p>Only the player whose {@code playerId} matches the receiving client side
 * ever sees a populated value. The opponent sees {@code null} so deck contents
 * are not leaked.
 */
@Data
public class PendingPlayerChoiceDTO {

    /** Choice type literal — drives which FE modal renders. */
    private String type;

    /** Cards the player may pick or reorder. Always face-up to the chooser. */
    private List<MatchStateDTO.CardViewDTO> options;

    private int minPick;
    private int maxPick;
    private String prompt;

    /** For {@code COIN_FLIP_INFO}: the predetermined heads/tails result to animate. */
    private Boolean coinFlipHeads;
}
