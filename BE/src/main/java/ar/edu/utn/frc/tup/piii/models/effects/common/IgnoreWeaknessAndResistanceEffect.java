package ar.edu.utn.frc.tup.piii.models.effects.common;

import ar.edu.utn.frc.tup.piii.models.AttackEffect;
import ar.edu.utn.frc.tup.piii.models.GameContext;

/**
 * Marks the current attack to ignore both Weakness and Resistance.
 *
 * <p>Used by: Mist Slash, Rock Wrecker.
 */
public class IgnoreWeaknessAndResistanceEffect implements AttackEffect {

    @Override
    public void apply(GameContext ctx) {
        ctx.setIgnoreWeakness(true);
        ctx.setIgnoreResistance(true);
    }
}
