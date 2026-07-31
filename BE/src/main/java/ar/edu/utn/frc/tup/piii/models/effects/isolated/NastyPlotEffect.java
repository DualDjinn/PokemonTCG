package ar.edu.utn.frc.tup.piii.models.effects.isolated;

import ar.edu.utn.frc.tup.piii.models.AttackEffect;
import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.GameContext;

/**
 * Zorua — Nasty Plot: search the attacker's deck for any one card, put it into the hand,
 * then shuffle the deck.
 *
 * <p>The card selection is a player interaction; this implementation takes the first card
 * in the deck as a placeholder until the engine layer wires in the selection prompt.
 */
public class NastyPlotEffect implements AttackEffect {

    @Override
    public void apply(GameContext ctx) {
        if (!ctx.getAttackerDeck().isEmpty()) {
            Card chosen = ctx.getAttackerDeck().remove(0);
            ctx.getAttackerHand().add(chosen);
        }
        ctx.shuffleAttackerDeck();
    }
}
