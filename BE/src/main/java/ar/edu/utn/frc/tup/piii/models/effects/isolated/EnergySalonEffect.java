package ar.edu.utn.frc.tup.piii.models.effects.isolated;

import java.util.HashSet;
import java.util.Set;

import ar.edu.utn.frc.tup.piii.models.AttackEffect;
import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.EnergyCard;
import ar.edu.utn.frc.tup.piii.models.EnergyType;
import ar.edu.utn.frc.tup.piii.models.GameContext;

/**
 * Delcatty — Energy Salon: search the deck for 3 cards of different basic Energy types,
 * reveal them, and put them into the hand. Shuffle afterward.
 */
public class EnergySalonEffect implements AttackEffect {

    private static final int TARGET_COUNT = 3;

    @Override
    public void apply(GameContext ctx) {
        Set<EnergyType> found = new HashSet<>();
        for (int i = 0; i < TARGET_COUNT; i++) {
            EnergyType nextType = findNextDistinctType(ctx, found);
            if (nextType == null) {
                break;
            }
            Card card = ctx.searchAttackerDeck(
                    c -> c instanceof EnergyCard ec
                            && ec.getEnergyType() == nextType
                            && !found.contains(ec.getEnergyType()));
            if (card != null) {
                found.add(((EnergyCard) card).getEnergyType());
                ctx.getAttackerHand().add(card);
            }
        }
        ctx.shuffleAttackerDeck();
    }

    private EnergyType findNextDistinctType(GameContext ctx, Set<EnergyType> alreadyFound) {
        for (Card c : ctx.getAttackerDeck()) {
            if (c instanceof EnergyCard ec
                    && ec.getEnergyType() != null
                    && ec.getEnergyType() != EnergyType.COLORLESS
                    && !alreadyFound.contains(ec.getEnergyType())) {
                return ec.getEnergyType();
            }
        }
        return null;
    }
}
