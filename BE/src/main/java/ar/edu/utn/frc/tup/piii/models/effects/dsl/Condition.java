package ar.edu.utn.frc.tup.piii.models.effects.dsl;

import ar.edu.utn.frc.tup.piii.models.GameContext;

/**
 * A boolean predicate over the current {@link GameContext}, used to gate effects.
 *
 * <p>Consumed by {@link Effects#when(Condition)}. Concrete conditions live in {@link Conditions}.
 */
@FunctionalInterface
public interface Condition {

    /**
     * Evaluates this condition against the given battle state.
     *
     * @param ctx the live game context
     * @return {@code true} when the gated effects should run
     */
    boolean test(GameContext ctx);
}
