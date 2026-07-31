package ar.edu.utn.frc.tup.piii.models.effects.common;

import ar.edu.utn.frc.tup.piii.models.AttackEffect;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.InPlayPokemon;

/**
 * Heals a fixed amount of damage from one of the attacker's Benched Pokémon.
 * The player chooses which benched Pokémon; this implementation heals the first
 * available as a placeholder until interactive selection is wired in.
 *
 * <p>Used by: Massage (60).
 */
public class HealBenchedEffect implements AttackEffect {

    private final int amount;

    public HealBenchedEffect(int amount) {
        this.amount = amount;
    }

    @Override
    public void apply(GameContext ctx) {
        if (ctx.getAttackerBench().isEmpty()) {
            return;
        }
        InPlayPokemon target = ctx.getAttackerBench().get(0);
        target.setDamageTaken(Math.max(0, target.getDamageTaken() - amount));
    }
}
