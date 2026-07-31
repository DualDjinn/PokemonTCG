package ar.edu.utn.frc.tup.piii.models.effects.common;

import ar.edu.utn.frc.tup.piii.models.AttackEffect;
import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.EnergyType;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.PokemonCard;

/**
 * Searches the attacker's deck for a Pokémon of the given type, reveals it, and puts it
 * into the attacker's hand. Shuffles the deck afterward.
 *
 * <p>Used by: Pheromotion (GRASS).
 */
public class SearchDeckPokemonToHandEffect implements AttackEffect {

    private final EnergyType typeFilter;

    public SearchDeckPokemonToHandEffect(EnergyType typeFilter) {
        this.typeFilter = typeFilter;
    }

    @Override
    public void apply(GameContext ctx) {
        Card found = ctx.searchAttackerDeck(
                c -> c instanceof PokemonCard pk
                        && (typeFilter == null || pk.getPokemonType() == typeFilter));
        if (found != null) {
            ctx.getAttackerHand().add(found);
        }
        ctx.shuffleAttackerDeck();
    }
}
