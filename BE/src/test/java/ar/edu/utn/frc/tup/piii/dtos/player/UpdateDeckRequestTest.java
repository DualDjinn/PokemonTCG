package ar.edu.utn.frc.tup.piii.dtos.player;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class UpdateDeckRequestTest {

    @Test
    void testSettersAndGetters() {
        UpdateDeckRequest request = new UpdateDeckRequest();
        request.setCardIds(List.of("xy1-1", "xy1-2"));

        assertThat(request.getCardIds()).containsExactly("xy1-1", "xy1-2");
    }

    @Test
    void testNoArgsConstructor() {
        assertThat(new UpdateDeckRequest()).isNotNull();
    }
}
