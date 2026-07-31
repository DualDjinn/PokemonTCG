package ar.edu.utn.frc.tup.piii.models.effects.isolated;

import ar.edu.utn.frc.tup.piii.models.AttackEffect;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.InPlayPokemon;

/**
 * Spritzee — Sweet Scent: heal 20 damage from one of the attacker's Pokémon.
 *
 * <p>Target selection is a player interaction; this implementation heals the attacker.
 */
public class SweetScentEffect implements AttackEffect {

    private static final int HEAL = 20;

    @Override
    public void apply(GameContext ctx) {
        InPlayPokemon target = ctx.getAttacker();
        target.setDamageTaken(Math.max(0, target.getDamageTaken() - HEAL));
    }
}
