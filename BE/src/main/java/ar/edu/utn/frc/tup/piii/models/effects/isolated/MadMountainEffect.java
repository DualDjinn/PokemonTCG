package ar.edu.utn.frc.tup.piii.models.effects.isolated;

import ar.edu.utn.frc.tup.piii.models.AttackEffect;
import ar.edu.utn.frc.tup.piii.models.GameContext;

/**
 * Rhydon — Mad Mountain: flip 2 coins; if both are heads, discard the top card of the
 * opponent's deck for each damage counter on this Pokémon.
 */
public class MadMountainEffect implements AttackEffect {

    private static final int COINS = 2;

    @Override
    public void apply(GameContext ctx) {
        int heads = ctx.flipCoins(COINS);
        if (heads == COINS) {
            int counters = ctx.attackerDamageCounters();
            for (int i = 0; i < counters; i++) {
                ctx.defenderDiscardTopCard();
            }
        }
    }
}
