package ar.edu.utn.frc.tup.piii.models.effects.isolated;

import ar.edu.utn.frc.tup.piii.models.AttackEffect;
import ar.edu.utn.frc.tup.piii.models.EnergyCard;
import ar.edu.utn.frc.tup.piii.models.GameContext;

/**
 * Zoroark — Night Claw: flip a coin; if tails discard 2 Energy attached to this Pokémon.
 */
public class NightClawEffect implements AttackEffect {

    private static final int DISCARD_COUNT = 2;

    @Override
    public void apply(GameContext ctx) {
        if (!ctx.flipCoin("nightClaw")) {
            for (int i = 0; i < DISCARD_COUNT; i++) {
                EnergyCard removed = ctx.getAttacker().removeOneEnergy(null);
                if (removed != null) {
                    ctx.getAttackerDiscard().add(removed);
                }
            }
        }
    }
}
