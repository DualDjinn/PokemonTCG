package ar.edu.utn.frc.tup.piii.models.effects.trainer;

import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.TrainerEffect;

/**
 * Fairy Garden (xy1-117): Each Pokémon that has any Fairy Energy attached
 * to it (both yours and your opponent's) has no Retreat Cost.
 *
 * <p>This effect is checked by the engine when validating retreats.
 * Playing the Stadium simply places it in the active stadium zone;
 * no immediate side-effect is needed.
 */
public class FairyGardenEffect implements TrainerEffect {

    @Override
    public void apply(GameContext ctx) {
        // Marker effect: the zero-retreat-cost check lives in RuleValidator.canRetreat.
        // TurnManager handles placement into the active stadium slot.
    }
}
