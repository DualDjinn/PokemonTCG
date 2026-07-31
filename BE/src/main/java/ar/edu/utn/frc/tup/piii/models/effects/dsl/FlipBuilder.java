package ar.edu.utn.frc.tup.piii.models.effects.dsl;

import ar.edu.utn.frc.tup.piii.models.AttackEffect;
import ar.edu.utn.frc.tup.piii.models.GameContext;

/**
 * Combinator that flips a single coin and runs one branch on heads and another on tails.
 *
 * <p>Replaces the family of hand-written coin-flip effects (Quick Attack, Dynamic Punch,
 * Distortion Beam, …). Both branches default to a no-op, so {@code flip().onHeads(...)}
 * with no {@code onTails} simply does nothing on tails.
 */
public final class FlipBuilder implements AttackEffect {

    private AttackEffect onHeads = Effects.seq();
    private AttackEffect onTails = Effects.seq();

    /**
     * Sets the effects applied when the coin lands heads.
     *
     * @param effects effects to run, in order, on heads
     * @return this builder, for chaining
     */
    public FlipBuilder onHeads(AttackEffect... effects) {
        this.onHeads = Effects.seq(effects);
        return this;
    }

    /**
     * Sets the effects applied when the coin lands tails.
     *
     * @param effects effects to run, in order, on tails
     * @return this builder, for chaining
     */
    public FlipBuilder onTails(AttackEffect... effects) {
        this.onTails = Effects.seq(effects);
        return this;
    }

    @Override
    public void apply(GameContext ctx) {
        // Use the event-emitting variant so the FE coin-flip overlay renders.
        if (ctx.flipCoin("attack")) {
            onHeads.apply(ctx);
        } else {
            onTails.apply(ctx);
        }
    }
}
