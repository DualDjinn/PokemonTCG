package ar.edu.utn.frc.tup.piii.models.effects.common;

import ar.edu.utn.frc.tup.piii.models.AttackEffect;
import ar.edu.utn.frc.tup.piii.models.GameContext;

/**
 * Draws a fixed number of cards from the attacker's deck into their hand.
 *
 * <p>Used by: Filch (1), Me First (1), Double Draw (2).
 */
public class DrawCardsEffect implements AttackEffect {

    private final int count;

    public DrawCardsEffect(int count) {
        this.count = count;
    }

    @Override
    public void apply(GameContext ctx) {
        for (int i = 0; i < count; i++) {
            ctx.attackerDrawCard();
        }
    }
}
