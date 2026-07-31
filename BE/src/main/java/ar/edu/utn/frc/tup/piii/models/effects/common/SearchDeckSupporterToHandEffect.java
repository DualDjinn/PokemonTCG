package ar.edu.utn.frc.tup.piii.models.effects.common;

import ar.edu.utn.frc.tup.piii.models.AttackEffect;
import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.SupporterCard;

/**
 * Searches the attacker's deck for up to {@code count} Supporter cards, reveals them,
 * and puts them into the attacker's hand. Shuffles the deck afterward.
 *
 * <p>Used by: Lead/Skiddo (1), Lead/Gogoat (2).
 */
public class SearchDeckSupporterToHandEffect implements AttackEffect {

    private final int count;

    public SearchDeckSupporterToHandEffect(int count) {
        this.count = count;
    }

    @Override
    public void apply(GameContext ctx) {
        int found = 0;
        while (found < count) {
            Card card = ctx.searchAttackerDeck(c -> c instanceof SupporterCard);
            if (card == null) {
                break;
            }
            ctx.getAttackerHand().add(card);
            found++;
        }
        ctx.shuffleAttackerDeck();
    }
}
