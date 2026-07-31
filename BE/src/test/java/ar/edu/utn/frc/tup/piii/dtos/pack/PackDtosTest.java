package ar.edu.utn.frc.tup.piii.dtos.pack;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ar.edu.utn.frc.tup.piii.dtos.player.CardSummaryDTO;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke tests para los DTOs de la tienda de sobres:
 * {@link PackInfoDTO}, {@link OpenPackRequest}, {@link OpenedPackResponse}.
 *
 * <p>Validan constructores, getters/setters y la integración entre DTOs.
 */
class PackDtosTest {

    // ============================ PackInfoDTO ============================

    @Test
    void packInfoDTONoArgsConstructorCreatesEmpty() {
        PackInfoDTO dto = new PackInfoDTO();
        assertThat(dto.getType()).isNull();
        assertThat(dto.getPrice()).isZero();
        assertThat(dto.getCardsPerPack()).isZero();
        assertThat(dto.getProbabilities()).isNull();
    }

    @Test
    void packInfoDTOAllArgsConstructorSetsAllFields() {
        Map<String, Double> probs = Map.of("HIGH", 0.8, "MEDIUM", 0.15, "LOW", 0.05);
        PackInfoDTO dto = new PackInfoDTO("BRONZE", 100, 3, probs);

        assertThat(dto.getType()).isEqualTo("BRONZE");
        assertThat(dto.getPrice()).isEqualTo(100);
        assertThat(dto.getCardsPerPack()).isEqualTo(3);
        assertThat(dto.getProbabilities()).containsAllEntriesOf(probs);
    }

    @Test
    void packInfoDTOEqualsAndHashCode() {
        Map<String, Double> probs = Map.of("HIGH", 0.5);
        PackInfoDTO a = new PackInfoDTO("X", 1, 1, probs);
        PackInfoDTO b = new PackInfoDTO("X", 1, 1, probs);
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    // ============================ OpenPackRequest ============================

    @Test
    void openPackRequestNoArgsConstructor() {
        OpenPackRequest req = new OpenPackRequest();
        assertThat(req.getPackType()).isNull();
    }

    @Test
    void openPackRequestAllArgsConstructor() {
        OpenPackRequest req = new OpenPackRequest("GOLD");
        assertThat(req.getPackType()).isEqualTo("GOLD");
    }

    @Test
    void openPackRequestSetter() {
        OpenPackRequest req = new OpenPackRequest();
        req.setPackType("SILVER");
        assertThat(req.getPackType()).isEqualTo("SILVER");
    }

    // ============================ OpenedPackResponse ============================

    @Test
    void openedPackResponseAllArgsConstructor() {
        CardSummaryDTO card = new CardSummaryDTO();
        card.setId("c1");
        card.setName("Test");
        card.setSupertype("Pokemon");

        OpenedPackResponse resp = new OpenedPackResponse(
                "BRONZE", List.of(card), 250);

        assertThat(resp.getPackType()).isEqualTo("BRONZE");
        assertThat(resp.getCards()).hasSize(1);
        assertThat(resp.getCards().get(0).getId()).isEqualTo("c1");
        assertThat(resp.getRemainingCoins()).isEqualTo(250);
    }

    @Test
    void openedPackResponseSetters() {
        OpenedPackResponse resp = new OpenedPackResponse();
        resp.setPackType("GOLD");
        resp.setCards(List.of());
        resp.setRemainingCoins(0);

        assertThat(resp.getPackType()).isEqualTo("GOLD");
        assertThat(resp.getCards()).isEmpty();
        assertThat(resp.getRemainingCoins()).isZero();
    }

    @Test
    void openedPackResponseEqualsAndHashCode() {
        OpenedPackResponse a = new OpenedPackResponse("X", List.of(), 0);
        OpenedPackResponse b = new OpenedPackResponse("X", List.of(), 0);
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }
}
