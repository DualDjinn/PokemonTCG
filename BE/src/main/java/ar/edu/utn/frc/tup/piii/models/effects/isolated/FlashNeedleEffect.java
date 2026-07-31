package ar.edu.utn.frc.tup.piii.models.effects.isolated;

import ar.edu.utn.frc.tup.piii.models.AttackEffect;
import ar.edu.utn.frc.tup.piii.models.GameContext;

/**
 * Beedrill — Flash Needle: flip 3 coins; damage = 40 × heads.
 * If all three are heads, prevent all effects of attacks (including damage) done to
 * this Pokémon during the opponent's next turn.
 */
public class FlashNeedleEffect implements AttackEffect {

    private static final int COINS = 3;
    // private static final int DAMAGE_PER_HEAD = 40;

    @Override
    public void apply(GameContext ctx) {
        int heads = ctx.flipCoins(COINS);
        ctx.setPendingDamage(ctx.getPendingDamage() * heads);
        if (heads == COINS) {
            ctx.getAttacker().setPreventAllEffects(true);
        }
    }
}
