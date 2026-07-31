package ar.edu.utn.frc.tup.piii.models.effects.trainer;

import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.TrainerEffect;

/**
 * Muscle Band (xy1-121): The attacks of the Pokémon this card is attached
 * to do 20 more damage to your opponent's Active Pokémon (before applying
 * Weakness and Resistance).
 *
 * <p>This effect is passive — the +20 damage is applied by the attack
 * resolution pipeline when the equipped Pokémon is attacking.
 * Attaching the tool is handled by TurnManager.
 */
public class MuscleBandEffect implements TrainerEffect {

    @Override
    public void apply(GameContext ctx) {
        // Marker effect: the +20 boost is applied in CalculateDamageHandler when
        // the attacker's tool is this card. TurnManager handles the attachment.
    }
}
