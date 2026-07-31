package ar.edu.utn.frc.tup.piii.models.effects.common;

import java.util.Arrays;
import java.util.List;

import ar.edu.utn.frc.tup.piii.models.AttackEffect;
import ar.edu.utn.frc.tup.piii.models.GameContext;

/**
 * Chains multiple {@link AttackEffect} instances, applying each in order.
 *
 * <p>Used whenever a single attack produces more than one distinct effect
 * (e.g. Crisis Vine = PARALYZED + POISONED; Dynamic Punch = +damage + CONFUSED).
 */
public class CompositeAttackEffect implements AttackEffect {

    private final List<AttackEffect> effects;

    public CompositeAttackEffect(AttackEffect... effects) {
        this.effects = Arrays.asList(effects);
    }

    public CompositeAttackEffect(List<AttackEffect> effects) {
        this.effects = List.copyOf(effects);
    }

    @Override
    public void apply(GameContext ctx) {
        for (AttackEffect effect : effects) {
            effect.apply(ctx);
        }
    }
}
