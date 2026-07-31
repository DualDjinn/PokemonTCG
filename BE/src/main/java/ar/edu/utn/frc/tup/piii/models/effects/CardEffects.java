package ar.edu.utn.frc.tup.piii.models.effects;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Container annotation for {@link CardEffect}. Authors should not use this type directly —
 * write multiple {@code @CardEffect(...)} annotations on the same element and the Java
 * compiler will fold them into this container per the {@link java.lang.annotation.Repeatable}
 * contract.
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CardEffects {

    /**
     * The grouped {@link CardEffect} occurrences for one annotated element.
     *
     * @return the array of card-effect declarations
     */
    CardEffect[] value();
}
