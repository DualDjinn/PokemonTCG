package ar.edu.utn.frc.tup.piii.dtos.player;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AddCardRequestTest {

    @Test
    void testSettersAndGetters() {
        AddCardRequest request = new AddCardRequest();
        request.setCardId("xy1-1");

        assertThat(request.getCardId()).isEqualTo("xy1-1");
    }

    @Test
    void testNoArgsConstructor() {
        assertThat(new AddCardRequest()).isNotNull();
    }
}
