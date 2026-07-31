package ar.edu.utn.frc.tup.piii.models.effects.common;

import ar.edu.utn.frc.tup.piii.models.AttackEffect;
import ar.edu.utn.frc.tup.piii.models.GameContext;

/**
 * Marks the current attack to ignore the defender's Resistance reduction.
 *
 * <p>Used by: Rock Tumble, Puncture (×2), Buster Swing.
 */
public class IgnoreResistanceEffect implements AttackEffect {

    @Override
    public void apply(GameContext ctx) {
        ctx.setIgnoreResistance(true);
    }
}
