package ar.edu.utn.frc.tup.piii.models.effects.abilities;

import ar.edu.utn.frc.tup.piii.models.Ability;
import ar.edu.utn.frc.tup.piii.models.AbilityEffect;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.InPlayPokemon;

/**
 * Generic PASSIVE ability: the holder's attacks deal {@code amount} more
 * damage (post-base, pre-Weakness).
 *
 * <p>Used as a scaffold for XY1 "power-up" abilities and tested in isolation
 * before any per-card wiring is added.
 */
public class BoostDamageAbility implements AbilityEffect {

    private final int amount;

    /**
     * Creates a new boost ability.
     *
     * @param amount non-negative damage bonus added to the attacker's outgoing
     *               damage for the duration of the resolution
     */
    public BoostDamageAbility(int amount) {
        this.amount = amount;
    }

    @Override
    public void apply(GameContext ctx, InPlayPokemon holder, Ability ability) {
        if (holder == null) {
            throw new IllegalArgumentException("holder must not be null");
        }
        ctx.setPendingDamage(ctx.getPendingDamage() + amount);
    }

    /**
     * Returns the bonus damage this ability contributes.
     *
     * @return configured damage boost
     */
    public int getAmount() {
        return amount;
    }
}
