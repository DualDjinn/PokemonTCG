package ar.edu.utn.frc.tup.piii.judge.events;

import java.util.List;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public final class DamageResolvedEvent extends JudgeEvent {

    private final String attackerCardId;
    private final String attackName;
    private final int baseDamage;
    private final int pendingDamageAfterEffect;
    private final boolean weaknessApplied;
    private final boolean resistanceApplied;
    private final boolean weaknessIgnored;
    private final boolean resistanceIgnored;
    private final List<String> modifiersApplied;
    private final int calculatedDamage;
    private final boolean preventedByAllEffects;
    private final boolean preventedByAllDamage;
    private final boolean preventedByThreshold;
    private final int thresholdValue;
    private final int reductionApplied;
    private final int finalDamage;
}
