package ar.edu.utn.frc.tup.piii.models.effects.isolated;

import ar.edu.utn.frc.tup.piii.models.AttackEffect;
import ar.edu.utn.frc.tup.piii.models.GameContext;

/**
 * Kakuna — Harden: during the opponent's next turn, if this Pokémon would be damaged by
 * an attack, prevent that damage if it is 60 or less.
 */
public class HardenEffect implements AttackEffect {

    private static final int THRESHOLD = 60;

    @Override
    public void apply(GameContext ctx) {
        ctx.getAttacker().setPreventIncomingDamageThreshold(THRESHOLD);
    }
}
