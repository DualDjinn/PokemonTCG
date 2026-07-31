package ar.edu.utn.frc.tup.piii.models.effects.common;

import java.util.List;

import ar.edu.utn.frc.tup.piii.models.AttackEffect;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.InPlayPokemon;

/**
 * Deals fixed damage to up to {@code targets} of the opponent's Benched Pokémon.
 * Weakness and Resistance do not apply to benched Pokémon.
 *
 * <p>Target selection is delegated to the game engine (player chooses); this effect
 * applies damage to the first {@code targets} available benched Pokémon as a placeholder
 * until interactive selection is implemented.
 *
 * <p>Used by: Mach Punch (10, 1), Break Through (30, 1), Tree Slam (20, 2),
 * Wild Barking (20, 1), Hydro Bombard (30, 2).
 */
public class SpreadBenchDamageEffect implements AttackEffect {

    private final int damage;
    private final int targets;

    public SpreadBenchDamageEffect(int damage, int targets) {
        this.damage = damage;
        this.targets = targets;
    }

    @Override
    public void apply(GameContext ctx) {
        List<InPlayPokemon> bench = ctx.getDefenderBench();
        int count = Math.min(targets, bench.size());
        for (int i = 0; i < count; i++) {
            ctx.dealDamageToBenched(bench.get(i), damage);
        }
    }
}
