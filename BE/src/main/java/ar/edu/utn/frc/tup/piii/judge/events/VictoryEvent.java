package ar.edu.utn.frc.tup.piii.judge.events;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public final class VictoryEvent extends JudgeEvent {

    private final String winnerPlayerId;
    private final String victoryReason;
}
