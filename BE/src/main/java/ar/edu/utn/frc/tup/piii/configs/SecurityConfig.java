package ar.edu.utn.frc.tup.piii.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Provides security-related beans used by services (PasswordEncoder).
 *
 * <p>Single responsibility: only exposes a PasswordEncoder bean used by service
 * implementations to hash and verify passwords. Uses BCrypt (one-way hashing).
 *
 * <p>Design note: BCrypt is intentionally one-way; verification must be done
 * with {@code passwordEncoder.matches(rawPassword, encodedPassword)}. There is
 * no decrypt operation.
 */
@Configuration
public class SecurityConfig {

    /**
     * Password encoder backed by BCrypt.
     * Default strength is acceptable for development; tune the strength if needed.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
