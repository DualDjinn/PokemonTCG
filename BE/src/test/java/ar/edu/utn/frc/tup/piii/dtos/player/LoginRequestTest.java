package ar.edu.utn.frc.tup.piii.dtos.player;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class LoginRequestTest {

    @Test
    void testEqualsAndHashCode_SameValues() {
        LoginRequest r1 = new LoginRequest();
        r1.setUsername("ash");
        r1.setPassword("pikachu123");

        LoginRequest r2 = new LoginRequest();
        r2.setUsername("ash");
        r2.setPassword("pikachu123");

        assertThat(r1).isEqualTo(r2);
        assertThat(r1.hashCode()).isEqualTo(r2.hashCode());
    }

    @Test
    void testEquals_DifferentUsername() {
        LoginRequest r1 = new LoginRequest();
        r1.setUsername("ash");

        LoginRequest r2 = new LoginRequest();
        r2.setUsername("misty");

        assertThat(r1).isNotEqualTo(r2);
    }

    @Test
    void testEquals_NullObject() {
        LoginRequest r = new LoginRequest();
        assertThat(r.equals(null)).isFalse();
    }

    @Test
    void testSettersAndGetters() {
        LoginRequest r = new LoginRequest();
        r.setUsername("ash");
        r.setPassword("secret");

        assertThat(r.getUsername()).isEqualTo("ash");
        assertThat(r.getPassword()).isEqualTo("secret");
    }

    @Test
    void testNoArgsConstructor() {
        assertThat(new LoginRequest()).isNotNull();
    }

    @Test
    void testHashCode_Consistency() {
        LoginRequest r = new LoginRequest();
        r.setUsername("ash");
        int hash = r.hashCode();
        assertThat(r.hashCode()).isEqualTo(hash);
    }
}
