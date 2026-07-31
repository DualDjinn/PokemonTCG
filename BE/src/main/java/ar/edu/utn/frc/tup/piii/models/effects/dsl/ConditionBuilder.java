package ar.edu.utn.frc.tup.piii.models.effects.dsl;

import ar.edu.utn.frc.tup.piii.models.AttackEffect;
import ar.edu.utn.frc.tup.piii.models.GameContext;

/**
 * Combinator that runs one branch when a {@link Condition} holds and an optional
 * branch otherwise.
 *
 * <p>Replaces "if the Defending Pokémon is …" / "if this Pokémon has …" style effects.
 */
public final class ConditionBuilder implements AttackEffect {

    private final Condition condition;
    private AttackEffect then = Effects.seq();
    private AttackEffect otherwise = Effects.seq();

    /**
     * Creates a builder gated on the given condition. Prefer {@link Effects#when(Condition)}.
     *
     * @param condition the predicate that gates the {@code then} branch
     */
    public ConditionBuilder(Condition condition) {
        this.condition = condition;
    }

    /**
     * Sets the effects applied when the condition holds.
     *
     * @param effects effects to run, in order, when the condition is true
     * @return this builder, for chaining
     */
    public ConditionBuilder then(AttackEffect... effects) {
        this.then = Effects.seq(effects);
        return this;
    }

    /**
     * Sets the effects applied when the condition does not hold.
     *
     * @param effects effects to run, in order, when the condition is false
     * @return this builder, for chaining
     */
    public ConditionBuilder otherwise(AttackEffect... effects) {
        this.otherwise = Effects.seq(effects);
        return this;
    }

    @Override
    public void apply(GameContext ctx) {
        if (condition.test(ctx)) {
            then.apply(ctx);
        } else {
            otherwise.apply(ctx);
        }
    }
}
