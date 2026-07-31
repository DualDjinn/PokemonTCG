package ar.edu.utn.frc.tup.piii.models.effects.common;

import java.util.List;

import ar.edu.utn.frc.tup.piii.models.AttackEffect;
import ar.edu.utn.frc.tup.piii.models.EnergyCard;
import ar.edu.utn.frc.tup.piii.models.EnergyType;
import ar.edu.utn.frc.tup.piii.models.GameContext;

/**
 * Discards energy attached to the attacking Pokémon.
 *
 * <p>If {@code discardAll} is {@code false}, removes exactly one energy matching
 * {@code filter} (any energy if filter is {@code null}).
 * If {@code discardAll} is {@code true}, removes all energies matching {@code filter}.
 *
 * <p>Used by: Flamethrower (null, false), Fireworks (null, false),
 * Fire Tail Slap (FIRE, false), Flare Blitz (FIRE, true), Thunderbolt (null, true),
 * Rainbow Spear (null, false).
 */
public class DiscardSelfEnergyEffect implements AttackEffect {

    private final EnergyType filter;
    private final boolean discardAll;

    public DiscardSelfEnergyEffect(EnergyType filter, boolean discardAll) {
        this.filter = filter;
        this.discardAll = discardAll;
    }

    @Override
    public void apply(GameContext ctx) {
        if (discardAll) {
            List<EnergyCard> removed = ctx.getAttacker().removeAllEnergies(filter);
            ctx.getAttackerDiscard().addAll(removed);
        } else {
            EnergyCard removed = ctx.getAttacker().removeOneEnergy(filter);
            if (removed != null) {
                ctx.getAttackerDiscard().add(removed);
            }
        }
    }
}
