package ar.edu.utn.frc.tup.piii.models.effects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Coverage guard rail for {@link CardEffectRegistry}.
 *
 * <p>For every Pokémon attack in {@code db.json} whose rulebook {@code text} is non-blank,
 * the registry must return a non-null {@link ar.edu.utn.frc.tup.piii.models.AttackEffect}.
 *
 * <p>Until the full XY1 set is wired up, the test uses a <strong>ratcheting allow-list</strong>:
 * it passes while the number of unwired non-vanilla attacks is at or below
 * {@link #MAX_KNOWN_GAPS}. Each wiring batch should lower the constant to match the new
 * actual count. If the count grows (regression), the test fails with the full list of
 * gaps so the offending card is obvious. When the constant reaches zero, this becomes
 * a hard guarantee that every printed effect has an implementation.
 */
class CardEffectCoverageTest {

    /**
     * Maximum tolerated number of Pokémon attacks that have a non-blank
     * rulebook text but no registered {@code AttackEffect}.
     *
     * <p>Lower this as cards get wired in {@link CardEffectRegistry}. Do not raise it
     * without justification — growth indicates a regression.
     */
    /**
     * Currently 1 known gap: {@code xy1-11[0] Torment} — requires the attacker to choose
     * which of the defender's attacks to lock for the next turn. That player-choice
     * mechanism is outside the attack-effect resolution layer and will be wired once
     * the action-pipeline supports interactive choices.
     */
    private static final int MAX_KNOWN_GAPS = 1;

    private CardEffectRegistry registry;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        registry = new CardEffectRegistry();
        objectMapper = new ObjectMapper();
    }

    @Test
    void nonVanillaAttacks_AreWired_OrCountedInRatchet() throws Exception {
        List<String> gaps = findUnwiredAttacks();
        if (gaps.size() > MAX_KNOWN_GAPS) {
            fail(("Unwired non-vanilla attacks (%d) exceeds MAX_KNOWN_GAPS (%d).%n"
                    + "Either wire the missing attacks in CardEffectRegistry or, if you "
                    + "intentionally raised the ratchet, update the constant.%n"
                    + "Full list:%n%s")
                    .formatted(gaps.size(), MAX_KNOWN_GAPS, String.join(System.lineSeparator(), gaps)));
        }
    }

    @Test
    void ratchet_IsNotStaleHigherThanReality() throws Exception {
        // If the ratchet is set higher than the real count, lower it.
        // This catches "we wired more cards but forgot to tighten the constant."
        int gaps = findUnwiredAttacks().size();
        assertThat(MAX_KNOWN_GAPS)
                .as("MAX_KNOWN_GAPS should match the actual gap count (was %d, ratchet %d)",
                        gaps, MAX_KNOWN_GAPS)
                .isLessThanOrEqualTo(Math.max(gaps, 0));
    }

    /**
     * Returns one human-readable line per Pokémon attack that needs an effect but
     * doesn't have one. Format: {@code "xy1-NN[i] AttackName — text…"}.
     */
    private List<String> findUnwiredAttacks() throws Exception {
        List<String> gaps = new ArrayList<>();
        try (InputStream is = getClass().getResourceAsStream("/data/db.json")) {
            JsonNode root = objectMapper.readTree(is);
            for (JsonNode card : root.get("data")) {
                if (!isPokemon(card)) {
                    continue;
                }
                String cardId = card.get("id").asText();
                JsonNode attacks = card.get("attacks");
                if (attacks == null || !attacks.isArray()) {
                    continue;
                }
                for (int i = 0; i < attacks.size(); i++) {
                    JsonNode atk = attacks.get(i);
                    String text = atk.hasNonNull("text") ? atk.get("text").asText().trim() : "";
                    if (text.isEmpty()) {
                        continue;   // vanilla — no effect required.
                    }
                    if (registry.getAttackEffect(cardId, i) != null) {
                        continue;   // already wired.
                    }
                    String name = atk.hasNonNull("name") ? atk.get("name").asText() : "?";
                    String preview = text.length() > 80 ? text.substring(0, 77) + "..." : text;
                    gaps.add("%s[%d] %s — %s".formatted(cardId, i, name, preview));
                }
            }
        }
        return gaps;
    }

    private boolean isPokemon(JsonNode card) {
        if (!card.hasNonNull("supertype")) {
            return false;
        }
        String s = card.get("supertype").asText();
        // db.json uses both "Pokemon" and "Pokémon" inconsistently across sources.
        return "Pokemon".equals(s) || "Pokémon".equals(s);
    }
}
