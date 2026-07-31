package ar.edu.utn.frc.tup.piii.judge.events;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public abstract class JudgeEvent {

    private final String engineMatchId;
    private final int turnNumber;
    private final String actingPlayerId;
    private final long timestamp;
    private final String eventCode;
}
