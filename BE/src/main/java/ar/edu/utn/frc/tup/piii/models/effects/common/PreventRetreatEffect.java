package ar.edu.utn.frc.tup.piii.models.effects.common;

import ar.edu.utn.frc.tup.piii.models.AttackEffect;
import ar.edu.utn.frc.tup.piii.models.GameContext;

/**
 * Prevents the defending Pokémon from retreating during the opponent's next turn.
 *
 * <p>Used by: Corner, and as part of Poison Ring.
 */
public class PreventRetreatEffect implements AttackEffect {

    @Override
    public void apply(GameContext ctx) {
        ctx.getDefender().setCantRetreat(true);
    }
}
