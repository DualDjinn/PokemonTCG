package ar.edu.utn.frc.tup.piii.dtos.judge;

import ar.edu.utn.frc.tup.piii.dtos.engine.GameActionType;
import lombok.Data;

@Data
public class JudgeQueryDTO {

    private String matchId;
    private String playerId;
    private JudgeQueryCategory category;
    private GameActionType targetAction;
    private String targetCardId;
    private String targetPokemonId;
    private Integer targetTurnNumber;
    private String lang;
}
