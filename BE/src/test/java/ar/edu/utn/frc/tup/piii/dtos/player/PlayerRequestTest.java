package ar.edu.utn.frc.tup.piii.dtos.player;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;

class PlayerRequestTest {

    @Test
    void testEqualsAndHashCode_SameValues() {
        PlayerRequest r1 = new PlayerRequest();
        r1.setUsername("ash");
        r1.setTrainerName("Ash Ketchum");
        r1.setEmail("ash@pokemon.com");
        r1.setPassword("pikachu123");
        r1.setDateOfBirth(LocalDate.of(2000, 1, 1));

        PlayerRequest r2 = new PlayerRequest();
        r2.setUsername("ash");
        r2.setTrainerName("Ash Ketchum");
        r2.setEmail("ash@pokemon.com");
        r2.setPassword("pikachu123");
        r2.setDateOfBirth(LocalDate.of(2000, 1, 1));

        assertThat(r1).isEqualTo(r2);
        assertThat(r1.hashCode()).isEqualTo(r2.hashCode());
    }

    @Test
    void testEquals_DifferentValues() {
        PlayerRequest r1 = new PlayerRequest();
        r1.setUsername("ash");

        PlayerRequest r2 = new PlayerRequest();
        r2.setUsername("misty");

        assertThat(r1).isNotEqualTo(r2);
    }

    @Test
    void testEquals_NullObject() {
        PlayerRequest r = new PlayerRequest();
        assertThat(r.equals(null)).isFalse();
    }

    @Test
    void testSettersAndGetters() {
        PlayerRequest r = new PlayerRequest();
        r.setUsername("ash");
        r.setTrainerName("Ash");
        r.setEmail("ash@test.com");
        r.setPassword("secret");
        r.setDateOfBirth(LocalDate.of(2000, 5, 10));

        assertThat(r.getUsername()).isEqualTo("ash");
        assertThat(r.getTrainerName()).isEqualTo("Ash");
        assertThat(r.getEmail()).isEqualTo("ash@test.com");
        assertThat(r.getPassword()).isEqualTo("secret");
        assertThat(r.getDateOfBirth()).isEqualTo(LocalDate.of(2000, 5, 10));
    }

    @Test
    void testNoArgsConstructor() {
        assertThat(new PlayerRequest()).isNotNull();
    }

    @Test
    void testHashCode_Consistency() {
        PlayerRequest r = new PlayerRequest();
        r.setUsername("ash");
        int hash = r.hashCode();
        assertThat(r.hashCode()).isEqualTo(hash);
    }
}
