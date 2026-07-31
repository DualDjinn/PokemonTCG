package ar.edu.utn.frc.tup.piii.judge.events;

import ar.edu.utn.frc.tup.piii.dtos.engine.GameActionType;
import java.util.List;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public final class ActionBlockedEvent extends JudgeEvent {

    private final GameActionType blockedAction;
    private final String targetCardId;
    private final String targetPokemonName;
    private final List<String> activeReasons;
}
