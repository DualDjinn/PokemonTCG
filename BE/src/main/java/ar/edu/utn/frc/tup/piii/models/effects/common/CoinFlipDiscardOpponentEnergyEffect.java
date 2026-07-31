package ar.edu.utn.frc.tup.piii.models.effects.common;

import ar.edu.utn.frc.tup.piii.models.AttackEffect;
import ar.edu.utn.frc.tup.piii.models.EnergyCard;
import ar.edu.utn.frc.tup.piii.models.GameContext;

/**
 * Flips a coin; on heads discards one energy from the opponent's Active Pokémon.
 *
 * <p>Used by: Eerie Impulse, Rip Claw, Crunch.
 */
public class CoinFlipDiscardOpponentEnergyEffect implements AttackEffect {

    @Override
    public void apply(GameContext ctx) {
        if (ctx.flipCoin("discardEnergy")) {
            EnergyCard removed = ctx.getDefender().removeOneEnergy(null);
            if (removed != null) {
                ctx.getDefenderDiscard().add(removed);
            }
        }
    }
}
