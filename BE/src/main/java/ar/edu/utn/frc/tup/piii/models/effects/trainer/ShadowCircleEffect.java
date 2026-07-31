package ar.edu.utn.frc.tup.piii.models.effects.trainer;

import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.TrainerEffect;

/**
 * Shadow Circle (xy1-126): Each Pokémon that has any Darkness Energy
 * attached to it (both yours and your opponent's) has no Weakness.
 *
 * <p>This effect is checked by the damage calculator during attack resolution.
 * Playing the Stadium simply places it in the active stadium zone;
 * no immediate side-effect is needed.
 */
public class ShadowCircleEffect implements TrainerEffect {

    @Override
    public void apply(GameContext ctx) {
        // Marker effect: the weakness-skip check lives in DamageCalculator.applyWeaknessStage.
        // TurnManager handles placement into the active stadium slot.
    }
}
