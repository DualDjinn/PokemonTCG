package ar.edu.utn.frc.tup.piii.models.effects.isolated;

import java.util.ArrayList;
import java.util.List;

import ar.edu.utn.frc.tup.piii.models.AttackEffect;
import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.GameContext;

/**
 * Malamar — Mental Trash: the opponent flips 4 coins; for each tails they discard
 * a card from their hand.
 */
public class MentalTrashEffect implements AttackEffect {

    private static final int COINS = 4;

    @Override
    public void apply(GameContext ctx) {
        int tails = COINS - ctx.flipCoins(COINS);
        int toDiscard = Math.min(tails, ctx.getDefenderHand().size());
        List<Card> discarded = new ArrayList<>();
        for (int i = 0; i < toDiscard; i++) {
            discarded.add(ctx.getDefenderHand().remove(0));
        }
        ctx.getDefenderDiscard().addAll(discarded);
    }
}
