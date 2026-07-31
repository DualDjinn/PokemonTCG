package ar.edu.utn.frc.tup.piii.dtos.engine;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@link CreateMatchRequest} DTO.
 */
class CreateMatchRequestTest {

    @Test
    void setterAndGetter_RoundTrip() {
        CreateMatchRequest request = new CreateMatchRequest();
        request.setDeckId("deck-1");
        assertThat(request.getDeckId()).isEqualTo("deck-1");
    }

    @Test
    void equalsHashCodeAndToString() {
        CreateMatchRequest a = new CreateMatchRequest();
        a.setDeckId("deck-1");
        CreateMatchRequest b = new CreateMatchRequest();
        b.setDeckId("deck-1");
        CreateMatchRequest other = new CreateMatchRequest();
        other.setDeckId("deck-2");

        assertThat(a).isEqualTo(b);
        assertThat(a).isEqualTo(a);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a).isNotEqualTo(other);
        assertThat(a).isNotEqualTo(null);
        assertThat(a).isNotEqualTo("string");
        assertThat(a.toString()).contains("deck-1");
    }
}
