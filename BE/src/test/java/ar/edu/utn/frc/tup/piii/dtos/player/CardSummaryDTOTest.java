package ar.edu.utn.frc.tup.piii.dtos.player;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CardSummaryDTOTest {

    @Test
    void testSettersAndGetters() {
        CardSummaryDTO dto = new CardSummaryDTO();
        dto.setId("xy1-1");
        dto.setName("Venusaur-EX");
        dto.setSupertype("Pokémon");
        dto.setImageUrl("http://example.com/small.jpg");
        dto.setImageUrlLarge("http://example.com/large.jpg");

        assertThat(dto.getId()).isEqualTo("xy1-1");
        assertThat(dto.getName()).isEqualTo("Venusaur-EX");
        assertThat(dto.getSupertype()).isEqualTo("Pokémon");
        assertThat(dto.getImageUrl()).isEqualTo("http://example.com/small.jpg");
        assertThat(dto.getImageUrlLarge()).isEqualTo("http://example.com/large.jpg");
    }

    @Test
    void testNoArgsConstructor() {
        assertThat(new CardSummaryDTO()).isNotNull();
    }
}
