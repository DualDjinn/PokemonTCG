package ar.edu.utn.frc.tup.piii.models.effects.isolated;

import ar.edu.utn.frc.tup.piii.models.AttackEffect;
import ar.edu.utn.frc.tup.piii.models.GameContext;

/**
 * Gourgeist — Spirit Scream: put damage counters on both Active Pokémon until each has
 * exactly 10 HP remaining.
 */
public class SpiritScreamEffect implements AttackEffect {

    private static final int MIN_HP = 10;

    @Override
    public void apply(GameContext ctx) {
        reduceToMinHp(ctx.getAttacker());
        reduceToMinHp(ctx.getDefender());
        ctx.setPendingDamage(0);
    }

    private void reduceToMinHp(ar.edu.utn.frc.tup.piii.models.InPlayPokemon target) {
        int maxHp = target.getCard().getHp();
        int targetDamage = maxHp - MIN_HP;
        if (target.getDamageTaken() < targetDamage) {
            target.setDamageTaken(targetDamage);
        }
    }
}
