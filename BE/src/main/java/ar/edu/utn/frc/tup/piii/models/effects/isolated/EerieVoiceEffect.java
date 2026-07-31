package ar.edu.utn.frc.tup.piii.models.effects.isolated;

import ar.edu.utn.frc.tup.piii.models.AttackEffect;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.InPlayPokemon;

/**
 * Gourgeist — Eerie Voice: put 2 damage counters (= 20 damage) on each of the opponent's Pokémon.
 */
public class EerieVoiceEffect implements AttackEffect {

    private static final int DAMAGE_PER_COUNTER = 10;
    private static final int COUNTERS = 2;
    private static final int DAMAGE = DAMAGE_PER_COUNTER * COUNTERS;

    @Override
    public void apply(GameContext ctx) {
        ctx.getDefender().setDamageTaken(ctx.getDefender().getDamageTaken() + DAMAGE);
        for (InPlayPokemon benched : ctx.getDefenderBench()) {
            benched.setDamageTaken(benched.getDamageTaken() + DAMAGE);
        }
    }
}
