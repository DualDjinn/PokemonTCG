package ar.edu.utn.frc.tup.piii.dtos.player;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class DeckDTOTest {

    @Test
    void testSettersAndGetters() {
        CardSummaryDTO card = new CardSummaryDTO();
        card.setId("xy1-1");

        DeckDTO dto = new DeckDTO();
        dto.setId("deck-1");
        dto.setName("My Deck");
        dto.setCardCount(1);
        dto.setCards(List.of(card));
        dto.setValid(true);
        dto.setErrors(List.of());

        assertThat(dto.getId()).isEqualTo("deck-1");
        assertThat(dto.getName()).isEqualTo("My Deck");
        assertThat(dto.getCardCount()).isEqualTo(1);
        assertThat(dto.getCards()).hasSize(1);
        assertThat(dto.isValid()).isTrue();
        assertThat(dto.getErrors()).isEmpty();
    }

    @Test
    void testNoArgsConstructor() {
        assertThat(new DeckDTO()).isNotNull();
    }
}
