package ar.edu.utn.frc.tup.piii.models.effects;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an {@link ar.edu.utn.frc.tup.piii.models.AttackEffect} bean as the
 * implementation of a specific card's attack.
 *
 * <p>Apply to either a Spring-managed class (alongside {@code @Component})
 * or to a {@code @Bean} factory method inside a {@code @Configuration} class.
 * The {@code CardEffectRegistry} discovers all annotated beans at startup and
 * indexes them by {@code cardId + ":" + attackIndex}.
 *
 * <p>When one bean implements the same effect for multiple cards (e.g. a card
 * reprinted across sets), repeat this annotation — the container
 * {@link CardEffects} is created automatically by the compiler.
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(CardEffects.class)
@Documented
public @interface CardEffect {

    /** Card identifier (e.g. {@code "xy1-9"}). */
    String cardId();

    /** Zero-based index of the attack on that card. */
    int attackIndex();
}
