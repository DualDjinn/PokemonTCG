package ar.edu.utn.frc.tup.piii.models.effects.common;

import ar.edu.utn.frc.tup.piii.models.AttackEffect;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.InPlayPokemon;

/**
 * Forces the opponent to switch their Active Pokémon with one of their Benched Pokémon.
 * The opponent chooses which benched Pokémon; this implementation swaps with the first
 * available as a placeholder until interactive selection is wired in.
 *
 * <p>Used by: Knock Back, Luring Glow, Rapid Spin (partial).
 */
public class SwitchOpponentEffect implements AttackEffect {

    @Override
    public void apply(GameContext ctx) {
        if (ctx.getDefenderBench().isEmpty()) {
            return;
        }
        InPlayPokemon newActive = ctx.getDefenderBench().remove(0);
        ctx.getDefenderBench().add(0, ctx.getDefender());
        ctx.setDefender(newActive);
    }
}
