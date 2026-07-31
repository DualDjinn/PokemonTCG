package ar.edu.utn.frc.tup.piii.models.effects.isolated;

import ar.edu.utn.frc.tup.piii.models.AttackEffect;
import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.GameContext;

/**
 * Simipour — Recycle: put one card from the attacker's discard pile on top of their deck.
 *
 * <p>Card selection is a player interaction; this implementation takes the last card
 * added to the discard as a placeholder.
 */
public class RecycleEffect implements AttackEffect {

    @Override
    public void apply(GameContext ctx) {
        if (ctx.getAttackerDiscard().isEmpty()) {
            return;
        }
        Card chosen = ctx.getAttackerDiscard().remove(ctx.getAttackerDiscard().size() - 1);
        ctx.getAttackerDeck().add(0, chosen);
    }
}
