package ar.edu.utn.frc.tup.piii.models.effects.isolated;

import ar.edu.utn.frc.tup.piii.models.AttackEffect;
import ar.edu.utn.frc.tup.piii.models.EnergyCard;
import ar.edu.utn.frc.tup.piii.models.GameContext;

/**
 * Emolga-EX — Electron Crush: the attacker may discard one Energy attached to this Pokémon;
 * if they do, this attack does 30 more damage.
 *
 * <p>This implementation always takes the discard option when energy is available; the engine
 * layer will add the optional player prompt.
 */
public class ElectronCrushEffect implements AttackEffect {

    private static final int BONUS = 30;

    @Override
    public void apply(GameContext ctx) {
        EnergyCard removed = ctx.getAttacker().removeOneEnergy(null);
        if (removed != null) {
            ctx.getAttackerDiscard().add(removed);
            ctx.setPendingDamage(ctx.getPendingDamage() + BONUS);
        }
    }
}
