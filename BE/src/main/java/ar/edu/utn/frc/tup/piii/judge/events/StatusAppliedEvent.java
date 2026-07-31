package ar.edu.utn.frc.tup.piii.judge.events;

import ar.edu.utn.frc.tup.piii.models.StatusCondition;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public final class StatusAppliedEvent extends JudgeEvent {

    private final String targetPokemonId;
    private final String targetPokemonName;
    private final StatusCondition status;
    private final boolean wasCured;
    private final String cureReason;
    private final String sourceAttackName;
    private final String sourceCardId;
}
