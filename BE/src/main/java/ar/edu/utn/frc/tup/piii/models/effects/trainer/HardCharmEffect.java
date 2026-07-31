package ar.edu.utn.frc.tup.piii.models.effects.trainer;

import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.TrainerEffect;

/**
 * Hard Charm (xy1-119): Any damage done to the Pokémon this card is
 * attached to by an opponent's attack is reduced by 20 (after applying
 * Weakness and Resistance).
 *
 * <p>This effect is passive — the damage reduction is checked by the
 * attack resolution pipeline when the equipped Pokémon is defending.
 * Attaching the tool is handled by TurnManager.
 */
public class HardCharmEffect implements TrainerEffect {

    @Override
    public void apply(GameContext ctx) {
        // Marker effect: the -20 reduction is applied in CalculateDamageHandler when
        // the defender's tool is this card. TurnManager handles the attachment.
    }
}
