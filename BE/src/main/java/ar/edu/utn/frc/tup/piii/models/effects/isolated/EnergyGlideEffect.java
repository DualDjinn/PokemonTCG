package ar.edu.utn.frc.tup.piii.models.effects.isolated;

import ar.edu.utn.frc.tup.piii.models.AttackEffect;
import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.EnergyCard;
import ar.edu.utn.frc.tup.piii.models.EnergyType;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.InPlayPokemon;

/**
 * Emolga-EX — Energy Glide: search the deck for a Lightning Energy and attach it to this
 * Pokémon; if an Energy was attached in this way, switch this Pokémon with one Benched Pokémon.
 */
public class EnergyGlideEffect implements AttackEffect {

    @Override
    public void apply(GameContext ctx) {
        Card found = ctx.searchAttackerDeck(
                c -> c instanceof EnergyCard ec && ec.getEnergyType() == EnergyType.LIGHTNING);
        if (found instanceof EnergyCard energy) {
            ctx.getAttacker().getEnergies().add(energy);
            ctx.shuffleAttackerDeck();
            if (!ctx.getAttackerBench().isEmpty()) {
                InPlayPokemon newActive = ctx.getAttackerBench().remove(0);
                ctx.getAttackerBench().add(0, ctx.getAttacker());
                ctx.setAttacker(newActive);
            }
        } else {
            ctx.shuffleAttackerDeck();
        }
    }
}
