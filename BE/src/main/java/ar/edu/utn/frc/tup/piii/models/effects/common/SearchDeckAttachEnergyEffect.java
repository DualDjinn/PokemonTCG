package ar.edu.utn.frc.tup.piii.models.effects.common;

import ar.edu.utn.frc.tup.piii.models.AttackEffect;
import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.EnergyCard;
import ar.edu.utn.frc.tup.piii.models.EnergyType;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.InPlayPokemon;

/**
 * Searches the attacker's deck for an energy card of the given type and attaches it
 * to the specified target, then shuffles the deck.
 *
 * <p>Used by: Flame Charge (FIRE → self), Gather Energy (any → any own Pokémon).
 * For Gather Energy the target is whichever own Pokémon the player chooses; this
 * implementation attaches to the attacker as a placeholder until interactive
 * selection is wired in.
 */
public class SearchDeckAttachEnergyEffect implements AttackEffect {

    private final EnergyType energyType;
    /** If {@code true}, attaches to the attacker; if {@code false}, player selects a target. */
    private final boolean attachToSelf;

    public SearchDeckAttachEnergyEffect(EnergyType energyType, boolean attachToSelf) {
        this.energyType = energyType;
        this.attachToSelf = attachToSelf;
    }

    @Override
    public void apply(GameContext ctx) {
        Card found = ctx.searchAttackerDeck(
                c -> c instanceof EnergyCard ec
                        && (energyType == null || ec.getEnergyType() == energyType));
        if (found instanceof EnergyCard energy) {
            InPlayPokemon target = attachToSelf ? ctx.getAttacker() : ctx.getAttacker();
            target.getEnergies().add(energy);
        }
        ctx.shuffleAttackerDeck();
    }
}
