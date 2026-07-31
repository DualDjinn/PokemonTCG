package ar.edu.utn.frc.tup.piii.models.effects.trainer;

import ar.edu.utn.frc.tup.piii.models.EnergyCard;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.InPlayPokemon;
import ar.edu.utn.frc.tup.piii.models.TrainerEffect;

/**
 * Team Flare Grunt (xy1-129): Discard an Energy attached to your
 * opponent's Active Pokémon.
 */
public class TeamFlareGruntEffect implements TrainerEffect {

    @Override
    public void apply(GameContext ctx) {
        InPlayPokemon defender = ctx.getDefender();
        if (defender == null || defender.getEnergies().isEmpty()) {
            return;
        }
        EnergyCard removed = defender.removeOneEnergy(null);
        if (removed != null) {
            ctx.getDefenderDiscard().add(removed);
        }
    }
}
