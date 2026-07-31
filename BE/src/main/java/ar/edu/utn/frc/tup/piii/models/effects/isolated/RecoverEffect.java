package ar.edu.utn.frc.tup.piii.models.effects.isolated;

import ar.edu.utn.frc.tup.piii.models.AttackEffect;
import ar.edu.utn.frc.tup.piii.models.EnergyCard;
import ar.edu.utn.frc.tup.piii.models.GameContext;

/**
 * Starmie — Recover: discard one Energy attached to this Pokémon and heal all damage from it.
 */
public class RecoverEffect implements AttackEffect {

    @Override
    public void apply(GameContext ctx) {
        EnergyCard removed = ctx.getAttacker().removeOneEnergy(null);
        if (removed != null) {
            ctx.getAttackerDiscard().add(removed);
            ctx.getAttacker().setDamageTaken(0);
        }
    }
}
