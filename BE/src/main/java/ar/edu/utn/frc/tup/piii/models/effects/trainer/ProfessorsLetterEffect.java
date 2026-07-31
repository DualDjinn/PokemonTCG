package ar.edu.utn.frc.tup.piii.models.effects.trainer;

import ar.edu.utn.frc.tup.piii.engine.choice.ChoiceType;
import ar.edu.utn.frc.tup.piii.engine.choice.PendingPlayerChoice;
import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.EnergyCard;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.TrainerEffect;

import java.util.ArrayList;
import java.util.List;

/**
 * Professor's Letter (xy1-123): Search your deck for up to 2 basic Energy
 * cards, reveal them, and put them into your hand. Shuffle your deck afterward.
 *
 * <p>Raises a {@link ChoiceType#PICK_BASIC_ENERGIES_FROM_DECK} prompt with all
 * basic energies in the deck. The player picks 0–2.
 */
public class ProfessorsLetterEffect implements TrainerEffect {

    private static final int MAX_ENERGIES = 2;

    @Override
    public void apply(GameContext ctx) {
        List<Card> deck = ctx.getAttackerDeck();
        List<Card> energyOptions = new ArrayList<>();
        for (Card card : deck) {
            // XY1 set only ships basic energies; all EnergyCard instances qualify.
            if (card instanceof EnergyCard) {
                energyOptions.add(card);
            }
        }

        if (energyOptions.isEmpty()) {
            ctx.shuffleAttackerDeck();
            return;
        }

        int max = Math.min(MAX_ENERGIES, energyOptions.size());

        ctx.setPendingPlayerChoice(new PendingPlayerChoice(
                ctx.getAttackerPlayerId(),
                ChoiceType.PICK_BASIC_ENERGIES_FROM_DECK,
                energyOptions,
                0,
                max,
                "Professor's Letter: pick up to " + max + " basic energy cards",
                null,
                chosen -> {
                    for (String id : chosen) {
                        Card found = deck.stream()
                                .filter(c -> id.equals(c.getId()))
                                .findFirst().orElse(null);
                        if (found != null) {
                            deck.remove(found);
                            ctx.getAttackerHand().add(found);
                        }
                    }
                    ctx.shuffleAttackerDeck();
                }
        ));
    }
}
