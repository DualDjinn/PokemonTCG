package ar.edu.utn.frc.tup.piii.models;

/**
 * Strategy interface for attack effects.
 *
 * <p>Each implementation encodes the side effect of one attack (or a composed set
 * of effects via {@code CompositeAttackEffect}). The engine's Chain-of-Responsibility
 * pipeline calls {@link #apply(GameContext)} after base damage is set on the context.
 */
@FunctionalInterface
public interface AttackEffect {

    /**
     * Applies this effect to the current game state.
     *
     * @param ctx mutable battle context — effects read and write state here
     */
    void apply(GameContext ctx);
}
