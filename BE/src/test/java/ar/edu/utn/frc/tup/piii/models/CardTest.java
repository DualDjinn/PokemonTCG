package ar.edu.utn.frc.tup.piii.models;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CardTest {

    @Test
    void builder_SetsAllCardFields() {
        PokemonCard card = PokemonCard.builder()
                .id("xy1-1").name("Venusaur")
                .setCode("xy1").number("1")
                .rarity(CardRarity.RARE_HOLO)
                .imageUrl("http://example.com/small.png")
                .imageUrlLarge("http://example.com/large.png")
                .hp(100).stage(PokemonStage.STAGE_2)
                .build();

        assertThat(card.getId()).isEqualTo("xy1-1");
        assertThat(card.getName()).isEqualTo("Venusaur");
        assertThat(card.getSetCode()).isEqualTo("xy1");
        assertThat(card.getNumber()).isEqualTo("1");
        assertThat(card.getRarity()).isEqualTo(CardRarity.RARE_HOLO);
        assertThat(card.getImageUrl()).isEqualTo("http://example.com/small.png");
        assertThat(card.getImageUrlLarge()).isEqualTo("http://example.com/large.png");
        assertThat(card.getSupertype()).isEqualTo(Supertype.POKEMON);
    }

    @Test
    void builder_WithMinimalFields() {
        EnergyCard card = EnergyCard.builder()
                .id("energy-1").name("Grass Energy")
                .build();

        assertThat(card.getId()).isEqualTo("energy-1");
        assertThat(card.getName()).isEqualTo("Grass Energy");
        assertThat(card.getSetCode()).isNull();
        assertThat(card.getNumber()).isNull();
        assertThat(card.getRarity()).isNull();
        assertThat(card.getImageUrl()).isNull();
        assertThat(card.getImageUrlLarge()).isNull();
    }
}
