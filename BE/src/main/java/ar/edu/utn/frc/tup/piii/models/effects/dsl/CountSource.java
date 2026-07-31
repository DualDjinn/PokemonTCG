package ar.edu.utn.frc.tup.piii.models.effects.dsl;

import ar.edu.utn.frc.tup.piii.models.GameContext;

/**
 * Produces an integer count from the current {@link GameContext}.
 *
 * <p>Used as the variable input to scaling-damage effects such as
 * {@link Effects#addDamagePer(CountSource, int)} and {@link Effects#multiplyDamageBy(CountSource)}.
 * Concrete sources live in {@link Counts} (bench size, attached energy, coin heads, …),
 * so the {@code ×N / +per N} family of attacks collapses to a single shared abstraction.
 */
@FunctionalInterface
public interface CountSource {

    /**
     * Computes the count for the given battle state.
     *
     * @param ctx the live game context
     * @return the count this source represents
     */
    int count(GameContext ctx);
}
