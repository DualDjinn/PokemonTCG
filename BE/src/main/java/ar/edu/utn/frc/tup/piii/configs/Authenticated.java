package ar.edu.utn.frc.tup.piii.configs;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller method parameter as requiring an authenticated player.
 *
 * <p>The {@link AuthenticatedPlayerResolver} reads the {@code sessionId} cookie,
 * resolves the player through {@link ar.edu.utn.frc.tup.piii.services.PlayerService},
 * and injects the resulting {@link ar.edu.utn.frc.tup.piii.models.Player}. If the
 * cookie is missing or the session is invalid, the resolver responds with 401.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface Authenticated {
}
