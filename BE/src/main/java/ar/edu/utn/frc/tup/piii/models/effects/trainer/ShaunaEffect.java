package ar.edu.utn.frc.tup.piii.models.effects.trainer;

import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.TrainerEffect;

import java.util.List;

/**
 * Shauna (xy1-127): Shuffle your hand into your deck. Then, draw 5 cards.
 */
public class ShaunaEffect implements TrainerEffect {

    private static final int CARDS_TO_DRAW = 5;

    @Override
    public void apply(GameContext ctx) {
        List<Card> hand = ctx.getAttackerHand();
        ctx.getAttackerDeck().addAll(hand);
        hand.clear();
        ctx.shuffleAttackerDeck();

        for (int i = 0; i < CARDS_TO_DRAW; i++) {
            ctx.attackerDrawCard();
        }
    }
}
