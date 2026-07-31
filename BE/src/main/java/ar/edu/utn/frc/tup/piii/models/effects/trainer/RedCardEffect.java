package ar.edu.utn.frc.tup.piii.models.effects.trainer;

import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.TrainerEffect;

import java.util.List;

/**
 * Red Card (xy1-124): Your opponent shuffles his or her hand into his
 * or her deck and draws 4 cards.
 */
public class RedCardEffect implements TrainerEffect {

    private static final int CARDS_TO_DRAW = 4;

    @Override
    public void apply(GameContext ctx) {
        List<Card> defenderHand = ctx.getDefenderHand();
        ctx.getDefenderDeck().addAll(defenderHand);
        defenderHand.clear();
        ctx.shuffleDefenderDeck();

        for (int i = 0; i < CARDS_TO_DRAW; i++) {
            ctx.defenderDrawCard();
        }
    }
}
