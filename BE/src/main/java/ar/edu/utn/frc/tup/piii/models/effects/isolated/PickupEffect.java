package ar.edu.utn.frc.tup.piii.models.effects.isolated;

import ar.edu.utn.frc.tup.piii.models.AttackEffect;
import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.ItemCard;

/**
 * Diggersby — Pickup: put 2 Item cards from the attacker's discard pile into their hand.
 */
public class PickupEffect implements AttackEffect {

    private static final int COUNT = 2;

    @Override
    public void apply(GameContext ctx) {
        int found = 0;
        for (int i = ctx.getAttackerDiscard().size() - 1; i >= 0 && found < COUNT; i--) {
            Card c = ctx.getAttackerDiscard().get(i);
            if (c instanceof ItemCard) {
                ctx.getAttackerDiscard().remove(i);
                ctx.getAttackerHand().add(c);
                found++;
            }
        }
    }
}
