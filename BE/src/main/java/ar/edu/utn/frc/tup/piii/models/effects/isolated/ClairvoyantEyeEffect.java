package ar.edu.utn.frc.tup.piii.models.effects.isolated;

import ar.edu.utn.frc.tup.piii.models.AttackEffect;
import ar.edu.utn.frc.tup.piii.models.GameContext;

/**
 * Braixen — Clairvoyant Eye: look at the top 3 cards of the attacker's deck and put
 * them back in any order.
 *
 * <p>The ordering decision is a player interaction; the deck order is unchanged here
 * (no-op re-insert) as a placeholder until the engine handles the UI prompt.
 */
public class ClairvoyantEyeEffect implements AttackEffect {

    private static final int LOOK_COUNT = 3;

    @Override
    public void apply(GameContext ctx) {
        // The top LOOK_COUNT cards are visible to the player; the engine layer is
        // responsible for prompting the player to re-order them. No structural change
        // occurs here — order is committed by the engine interaction layer.
        if (ctx.getAttackerDeck().size() < LOOK_COUNT) {
            return;
        }
    }
}
