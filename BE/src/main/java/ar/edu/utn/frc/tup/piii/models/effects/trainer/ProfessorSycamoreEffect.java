package ar.edu.utn.frc.tup.piii.models.effects.trainer;

import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.TrainerEffect;

import java.util.List;

/**
 * Professor Sycamore (xy1-122): Discard your hand and draw 7 cards.
 */
public class ProfessorSycamoreEffect implements TrainerEffect {

    private static final int CARDS_TO_DRAW = 7;

    @Override
    public void apply(GameContext ctx) {
        List<Card> hand = ctx.getAttackerHand();
        ctx.getAttackerDiscard().addAll(hand);
        hand.clear();

        for (int i = 0; i < CARDS_TO_DRAW; i++) {
            ctx.attackerDrawCard();
        }
    }
}
