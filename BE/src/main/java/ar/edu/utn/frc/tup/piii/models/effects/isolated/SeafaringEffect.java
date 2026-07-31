package ar.edu.utn.frc.tup.piii.models.effects.isolated;

import ar.edu.utn.frc.tup.piii.models.AttackEffect;
import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.EnergyCard;
import ar.edu.utn.frc.tup.piii.models.EnergyType;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.InPlayPokemon;

/**
 * Lapras — Seafaring: flip 3 coins; for each heads, attach one Water Energy card from the
 * attacker's discard pile to any of the attacker's Benched Pokémon.
 *
 * <p>Targets the first available benched Pokémon as a placeholder for player selection.
 */
public class SeafaringEffect implements AttackEffect {

    private static final int COINS = 3;

    @Override
    public void apply(GameContext ctx) {
        int heads = ctx.flipCoins(COINS);
        for (int i = 0; i < heads; i++) {
            Card found = findWaterEnergyInDiscard(ctx);
            if (found == null || ctx.getAttackerBench().isEmpty()) {
                break;
            }
            ctx.getAttackerDiscard().remove(found);
            InPlayPokemon target = ctx.getAttackerBench().get(0);
            target.getEnergies().add((EnergyCard) found);
        }
    }

    private Card findWaterEnergyInDiscard(GameContext ctx) {
        for (Card c : ctx.getAttackerDiscard()) {
            if (c instanceof EnergyCard ec && ec.getEnergyType() == EnergyType.WATER) {
                return c;
            }
        }
        return null;
    }
}
