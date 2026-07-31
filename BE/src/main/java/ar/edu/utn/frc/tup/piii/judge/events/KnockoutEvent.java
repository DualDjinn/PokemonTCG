package ar.edu.utn.frc.tup.piii.judge.events;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public final class KnockoutEvent extends JudgeEvent {

    private final String knockedPokemonId;
    private final String knockedPokemonName;
    private final int damageTaken;
    private final int maxHp;
    private final int prizesAwarded;
}
