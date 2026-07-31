package ar.edu.utn.frc.tup.piii.models.effects.abilities;

import ar.edu.utn.frc.tup.piii.models.Ability;
import ar.edu.utn.frc.tup.piii.models.AbilityEffect;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.InPlayPokemon;

/**
 * Generic PASSIVE ability: the holder takes {@code amount} less damage from
 * incoming attacks, floored at {@code 0}.
 *
 * <p>Used as a scaffold for XY1 "armor" / "resistance-like" abilities. Note
 * that this is an ability, not a Resistance type — it stacks on top of the
 * rulebook's Resistance step.
 */
public class ReduceDamageAbility implements AbilityEffect {

    private final int amount;

    /**
     * Creates a new damage-reduction ability.
     *
     * @param amount non-negative damage subtracted from the holder's incoming
     *               damage for the duration of the resolution
     */
    public ReduceDamageAbility(int amount) {
        this.amount = amount;
    }

    @Override
    public void apply(GameContext ctx, InPlayPokemon holder, Ability ability) {
        if (holder == null) {
            throw new IllegalArgumentException("holder must not be null");
        }
        int next = ctx.getPendingDamage() - amount;
        ctx.setPendingDamage(Math.max(0, next));
    }

    /**
     * Returns the damage subtracted by this ability.
     *
     * @return configured damage reduction
     */
    public int getAmount() {
        return amount;
    }
}
