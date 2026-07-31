package ar.edu.utn.frc.tup.piii.models.effects.isolated;

import ar.edu.utn.frc.tup.piii.models.AttackEffect;
import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.GameContext;

/**
 * Phantump — Astonish: choose a random card from the opponent's hand; the opponent
 * reveals it and shuffles it into their deck.
 */
public class AstonishEffect implements AttackEffect {

    @Override
    public void apply(GameContext ctx) {
        if (ctx.getDefenderHand().isEmpty()) {
            return;
        }
        int index = ctx.getRandom().nextInt(ctx.getDefenderHand().size());
        Card chosen = ctx.getDefenderHand().remove(index);
        ctx.getDefenderDeck().add(chosen);
        ctx.shuffleDefenderDeck();
    }
}
