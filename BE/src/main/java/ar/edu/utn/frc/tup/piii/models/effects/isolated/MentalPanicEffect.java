package ar.edu.utn.frc.tup.piii.models.effects.isolated;

import ar.edu.utn.frc.tup.piii.models.AttackEffect;
import ar.edu.utn.frc.tup.piii.models.GameContext;

/**
 * Malamar — Mental Panic: if the defending Pokémon tries to attack during the opponent's
 * next turn, the opponent flips a coin; tails means that attack does nothing.
 */
public class MentalPanicEffect implements AttackEffect {

    @Override
    public void apply(GameContext ctx) {
        ctx.getDefender().setIfAttacksFlipCoin(true);
    }
}
