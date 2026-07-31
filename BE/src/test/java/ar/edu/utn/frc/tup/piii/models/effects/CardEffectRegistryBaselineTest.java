package ar.edu.utn.frc.tup.piii.models.effects;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Baseline parity test for {@link CardEffectRegistry}.
 *
 * <p>Locks in the exact set of {@code "cardId:attackIndex"} keys that the registry
 * must resolve. The expected set lives in the classpath resource
 * {@code card-effect-baseline-keys.txt} (160 keys). This test is intended to keep
 * passing across the Phase 1 → Phase 5 refactor of the registry into a Spring-driven
 * discoverer, proving zero regression on the effect catalogue.
 */
@SpringBootTest
class CardEffectRegistryBaselineTest {

    @Autowired
    private CardEffectRegistry registry;

    @Test
    void every_baseline_key_resolves() throws Exception {
        Set<String> expected = loadExpectedKeys();
        for (String key : expected) {
            String[] parts = key.split(":");
            assertThat(registry.getAttackEffect(parts[0], Integer.parseInt(parts[1])))
                .as("expected effect for key %s", key)
                .isNotNull();
        }
    }

    @Test
    void registered_keys_match_baseline_exactly() throws Exception {
        Set<String> expected = loadExpectedKeys();
        assertThat(registry.registeredKeys())
            .containsExactlyInAnyOrderElementsOf(expected);
    }

    private static Set<String> loadExpectedKeys() throws Exception {
        try (InputStream in = CardEffectRegistryBaselineTest.class
                .getResourceAsStream("/card-effect-baseline-keys.txt")) {
            assertThat(in).as("baseline resource missing").isNotNull();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8))) {
                return reader.lines()
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.toUnmodifiableSet());
            }
        }
    }
}
