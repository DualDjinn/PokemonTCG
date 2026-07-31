package ar.edu.utn.frc.tup.piii.models.effects.isolated;

import ar.edu.utn.frc.tup.piii.models.AttackEffect;
import ar.edu.utn.frc.tup.piii.models.EnergyCard;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.StatusCondition;

/**
 * Cloyster — Clamp Crush: flip a coin; if heads the defender is now Paralyzed and
 * one Energy attached to that Pokémon is discarded.
 */
public class ClampCrushEffect implements AttackEffect {

    @Override
    public void apply(GameContext ctx) {
        if (ctx.flipCoin("clampCrush")) {
            ctx.getDefender().applyStatus(StatusCondition.PARALYZED);
            EnergyCard removed = ctx.getDefender().removeOneEnergy(null);
            if (removed != null) {
                ctx.getDefenderDiscard().add(removed);
            }
        }
    }
}
