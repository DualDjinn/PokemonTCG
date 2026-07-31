package ar.edu.utn.frc.tup.piii.models.effects.isolated;

import ar.edu.utn.frc.tup.piii.models.AttackEffect;
import ar.edu.utn.frc.tup.piii.models.GameContext;

/**
 * Bisharp — Metal Wallop: during the next turn, this Pokémon's Metal Wallop attack
 * does 40 more damage (before W/R). Consecutive uses stack.
 *
 * <p>On application: read any existing bonus, add it to pendingDamage, then record
 * a new +40 bonus for the following turn.
 */
public class MetalWallopEffect implements AttackEffect {

    private static final int STACK_BONUS = 40;

    @Override
    public void apply(GameContext ctx) {
        int existingBonus = ctx.getAttacker().getMetalWallopBonus();
        ctx.setPendingDamage(ctx.getPendingDamage() + existingBonus);
        ctx.getAttacker().setMetalWallopBonus(existingBonus + STACK_BONUS);
    }
}
