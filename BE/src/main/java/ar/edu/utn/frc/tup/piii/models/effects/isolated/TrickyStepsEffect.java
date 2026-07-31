package ar.edu.utn.frc.tup.piii.models.effects.isolated;

import ar.edu.utn.frc.tup.piii.models.AttackEffect;
import ar.edu.utn.frc.tup.piii.models.EnergyCard;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.InPlayPokemon;

/**
 * Grumpig — Tricky Steps: the attacker may move one Energy attached to the opponent's
 * Active Pokémon to one of the opponent's Benched Pokémon (both choices by the attacker).
 *
 * <p>This implementation always moves if possible; the engine layer will add the optional
 * player prompt.
 */
public class TrickyStepsEffect implements AttackEffect {

    @Override
    public void apply(GameContext ctx) {
        if (ctx.getDefenderBench().isEmpty()) {
            return;
        }
        EnergyCard moved = ctx.getDefender().removeOneEnergy(null);
        if (moved != null) {
            InPlayPokemon target = ctx.getDefenderBench().get(0);
            target.getEnergies().add(moved);
        }
    }
}
