package ar.edu.utn.frc.tup.piii.models.effects.common;

import ar.edu.utn.frc.tup.piii.models.AttackEffect;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.InPlayPokemon;

/**
 * Switches the attacking Pokémon with one of the attacker's Benched Pokémon.
 * The player chooses which benched Pokémon; this implementation swaps with the first
 * available benched Pokémon as a placeholder until interactive selection is wired in.
 *
 * <p>Used by: Bounce (after coin heads), Energy Glide (after attaching), Rapid Spin (partial).
 */
public class SwitchSelfEffect implements AttackEffect {

    @Override
    public void apply(GameContext ctx) {
        if (ctx.getAttackerBench().isEmpty()) {
            return;
        }
        InPlayPokemon newActive = ctx.getAttackerBench().remove(0);
        ctx.getAttackerBench().add(0, ctx.getAttacker());
        ctx.setAttacker(newActive);
    }
}
