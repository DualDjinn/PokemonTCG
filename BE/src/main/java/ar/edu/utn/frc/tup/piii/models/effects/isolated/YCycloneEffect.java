package ar.edu.utn.frc.tup.piii.models.effects.isolated;

import ar.edu.utn.frc.tup.piii.models.AttackEffect;
import ar.edu.utn.frc.tup.piii.models.EnergyCard;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.InPlayPokemon;

/**
 * Yveltal-EX — Y Cyclone: move one Energy from this Pokémon to one of the attacker's
 * Benched Pokémon (player's choice; first benched Pokémon used as placeholder).
 */
public class YCycloneEffect implements AttackEffect {

    @Override
    public void apply(GameContext ctx) {
        if (ctx.getAttackerBench().isEmpty()) {
            return;
        }
        EnergyCard moved = ctx.getAttacker().removeOneEnergy(null);
        if (moved != null) {
            InPlayPokemon target = ctx.getAttackerBench().get(0);
            target.getEnergies().add(moved);
        }
    }
}
