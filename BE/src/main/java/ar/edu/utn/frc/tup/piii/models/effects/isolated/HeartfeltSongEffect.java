package ar.edu.utn.frc.tup.piii.models.effects.isolated;

import ar.edu.utn.frc.tup.piii.models.AttackEffect;
import ar.edu.utn.frc.tup.piii.models.EnergyCard;
import ar.edu.utn.frc.tup.piii.models.EnergyType;
import ar.edu.utn.frc.tup.piii.models.GameContext;

/**
 * Jigglypuff — Heartfelt Song: discard one Darkness Energy attached to the opponent's
 * Active Pokémon.
 */
public class HeartfeltSongEffect implements AttackEffect {

    @Override
    public void apply(GameContext ctx) {
        EnergyCard removed = ctx.getDefender().removeOneEnergy(EnergyType.DARKNESS);
        if (removed != null) {
            ctx.getDefenderDiscard().add(removed);
        }
    }
}
