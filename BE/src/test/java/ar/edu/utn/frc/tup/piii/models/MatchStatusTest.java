package ar.edu.utn.frc.tup.piii.models;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@link MatchStatus} enum.
 */
class MatchStatusTest {

    @Test
    void values_ContainsTheFourLifecycleStates() {
        assertThat(MatchStatus.values())
                .containsExactly(
                        MatchStatus.WAITING,
                        MatchStatus.SETUP,
                        MatchStatus.ACTIVE,
                        MatchStatus.FINISHED);
    }

    @Test
    void valueOf_ResolvesEachNameBackToItsConstant() {
        assertThat(MatchStatus.valueOf("WAITING")).isEqualTo(MatchStatus.WAITING);
        assertThat(MatchStatus.valueOf("SETUP")).isEqualTo(MatchStatus.SETUP);
        assertThat(MatchStatus.valueOf("ACTIVE")).isEqualTo(MatchStatus.ACTIVE);
        assertThat(MatchStatus.valueOf("FINISHED")).isEqualTo(MatchStatus.FINISHED);
    }
}
