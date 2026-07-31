package ar.edu.utn.frc.tup.piii.dtos.engine;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@link JoinMatchRequest} DTO.
 */
class JoinMatchRequestTest {

    @Test
    void setterAndGetter_RoundTrip() {
        JoinMatchRequest request = new JoinMatchRequest();
        request.setDeckId("deck-2");
        assertThat(request.getDeckId()).isEqualTo("deck-2");
    }

    @Test
    void equalsHashCodeAndToString() {
        JoinMatchRequest a = new JoinMatchRequest();
        a.setDeckId("deck-2");
        JoinMatchRequest b = new JoinMatchRequest();
        b.setDeckId("deck-2");
        JoinMatchRequest other = new JoinMatchRequest();
        other.setDeckId("deck-9");

        assertThat(a).isEqualTo(b);
        assertThat(a).isEqualTo(a);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a).isNotEqualTo(other);
        assertThat(a).isNotEqualTo(null);
        assertThat(a).isNotEqualTo("string");
        assertThat(a.toString()).contains("deck-2");
    }
}
