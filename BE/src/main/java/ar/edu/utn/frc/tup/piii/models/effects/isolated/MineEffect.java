package ar.edu.utn.frc.tup.piii.models.effects.isolated;

import ar.edu.utn.frc.tup.piii.models.AttackEffect;
import ar.edu.utn.frc.tup.piii.models.GameContext;

/**
 * Diglett — Mine: look at the top card of the opponent's deck; then the attacker may
 * have the opponent shuffle their deck.
 *
 * <p>The shuffle decision is a player interaction. This implementation always shuffles
 * (worst-case for the opponent) as a placeholder until the engine wires in the prompt.
 */
public class MineEffect implements AttackEffect {

    @Override
    public void apply(GameContext ctx) {
        if (ctx.getDefenderDeck().isEmpty()) {
            return;
        }
        // Top card is revealed (engine layer handles display); shuffle is optional by attacker.
        ctx.shuffleDefenderDeck();
    }
}
