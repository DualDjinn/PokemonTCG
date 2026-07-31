package ar.edu.utn.frc.tup.piii.models.effects.common;

import ar.edu.utn.frc.tup.piii.models.AttackEffect;
import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.EnergyCard;
import ar.edu.utn.frc.tup.piii.models.EnergyType;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.InPlayPokemon;

/**
 * Retrieves one energy card of the given type from the attacker's discard pile and
 * attaches it to a target Pokémon (self or a chosen benched Pokémon).
 *
 * <p>Used by: Oblivion Wing (DARKNESS → benched).
 */
public class AttachFromDiscardEffect implements AttackEffect {

    private final EnergyType energyType;
    private final boolean attachToBench;

    public AttachFromDiscardEffect(EnergyType energyType, boolean attachToBench) {
        this.energyType = energyType;
        this.attachToBench = attachToBench;
    }

    @Override
    public void apply(GameContext ctx) {
        Card found = null;
        for (int i = 0; i < ctx.getAttackerDiscard().size(); i++) {
            Card c = ctx.getAttackerDiscard().get(i);
            if (c instanceof EnergyCard ec && ec.getEnergyType() == energyType) {
                found = ctx.getAttackerDiscard().remove(i);
                break;
            }
        }
        if (found instanceof EnergyCard energy) {
            InPlayPokemon target = chooseTarget(ctx);
            if (target != null) {
                target.getEnergies().add(energy);
            }
        }
    }

    private InPlayPokemon chooseTarget(GameContext ctx) {
        if (!attachToBench) {
            return ctx.getAttacker();
        }
        return ctx.getAttackerBench().isEmpty() ? null : ctx.getAttackerBench().get(0);
    }
}
