package ar.edu.utn.frc.tup.piii.dtos.player;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class UpdateTrainerNameRequestTest {

    @Test
    void testEqualsAndHashCode_SameValues() {
        UpdateTrainerNameRequest r1 = new UpdateTrainerNameRequest();
        r1.setTrainerName("Ash Ketchum");

        UpdateTrainerNameRequest r2 = new UpdateTrainerNameRequest();
        r2.setTrainerName("Ash Ketchum");

        assertThat(r1).isEqualTo(r2);
        assertThat(r1.hashCode()).isEqualTo(r2.hashCode());
    }

    @Test
    void testEquals_DifferentValues() {
        UpdateTrainerNameRequest r1 = new UpdateTrainerNameRequest();
        r1.setTrainerName("Ash");

        UpdateTrainerNameRequest r2 = new UpdateTrainerNameRequest();
        r2.setTrainerName("Misty");

        assertThat(r1).isNotEqualTo(r2);
    }

    @Test
    void testSettersAndGetters() {
        UpdateTrainerNameRequest r = new UpdateTrainerNameRequest();
        r.setTrainerName("New Name");

        assertThat(r.getTrainerName()).isEqualTo("New Name");
    }

    @Test
    void testNoArgsConstructor() {
        assertThat(new UpdateTrainerNameRequest()).isNotNull();
    }

    @Test
    void testEquals_NullObject() {
        UpdateTrainerNameRequest r = new UpdateTrainerNameRequest();
        assertThat(r.equals(null)).isFalse();
    }
}
