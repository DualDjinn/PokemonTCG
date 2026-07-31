package ar.edu.utn.frc.tup.piii.models.effects.isolated;

import ar.edu.utn.frc.tup.piii.models.AttackEffect;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.PokemonToolCard;

/**
 * Skarmory-EX — Joust: before doing damage, discard all Pokémon Tool cards attached to
 * the opponent's Active Pokémon.
 */
public class JoustEffect implements AttackEffect {

    @Override
    public void apply(GameContext ctx) {
        if (ctx.getDefender().getTool() instanceof PokemonToolCard) {
            ctx.getDefenderDiscard().add(ctx.getDefender().getTool());
            ctx.getDefender().setTool(null);
        }
    }
}
