package ar.edu.utn.frc.tup.piii.models.effects.isolated;

import ar.edu.utn.frc.tup.piii.models.AttackEffect;
import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.EnergyCard;
import ar.edu.utn.frc.tup.piii.models.EnergyType;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.InPlayPokemon;

/**
 * Xerneas — Geomancy: choose 2 of the attacker's Benched Pokémon; for each, search
 * the deck for a Fairy Energy and attach it. Shuffle afterward.
 *
 * <p>This implementation targets the first 2 available benched Pokémon; the engine
 * layer will add the player selection prompt.
 */
public class GeomancyEffect implements AttackEffect {

    private static final int TARGETS = 2;

    @Override
    public void apply(GameContext ctx) {
        int count = Math.min(TARGETS, ctx.getAttackerBench().size());
        for (int i = 0; i < count; i++) {
            InPlayPokemon target = ctx.getAttackerBench().get(i);
            Card found = ctx.searchAttackerDeck(
                    c -> c instanceof EnergyCard ec && ec.getEnergyType() == EnergyType.FAIRY);
            if (found instanceof EnergyCard energy) {
                target.getEnergies().add(energy);
            }
        }
        ctx.shuffleAttackerDeck();
    }
}
