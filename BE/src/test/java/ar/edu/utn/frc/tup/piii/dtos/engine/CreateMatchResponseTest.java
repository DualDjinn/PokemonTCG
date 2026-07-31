package ar.edu.utn.frc.tup.piii.dtos.engine;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import ar.edu.utn.frc.tup.piii.models.MatchStatus;

/**
 * Unit tests for the {@link CreateMatchResponse} DTO.
 */
class CreateMatchResponseTest {

    private CreateMatchResponse build() {
        CreateMatchResponse response = new CreateMatchResponse();
        response.setMatchId("match-1");
        response.setJoinCode("ABC123");
        response.setStatus(MatchStatus.WAITING);
        return response;
    }

    @Test
    void settersAndGetters_RoundTrip() {
        CreateMatchResponse response = build();
        assertThat(response.getMatchId()).isEqualTo("match-1");
        assertThat(response.getJoinCode()).isEqualTo("ABC123");
        assertThat(response.getStatus()).isEqualTo(MatchStatus.WAITING);
    }

    @Test
    void equalsHashCodeAndToString() {
        CreateMatchResponse a = build();
        CreateMatchResponse b = build();
        CreateMatchResponse other = build();
        other.setStatus(MatchStatus.SETUP);

        assertThat(a).isEqualTo(b);
        assertThat(a).isEqualTo(a);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a).isNotEqualTo(other);
        assertThat(a).isNotEqualTo(null);
        assertThat(a).isNotEqualTo("string");
        assertThat(a.toString()).contains("match-1", "ABC123", "WAITING");
    }
}
