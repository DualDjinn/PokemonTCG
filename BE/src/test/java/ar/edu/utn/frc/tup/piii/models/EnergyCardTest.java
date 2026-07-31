package ar.edu.utn.frc.tup.piii.models;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class EnergyCardTest {

    @Test
    void builder_SetsFields() {
        EnergyCard card = EnergyCard.builder()
                .id("energy-1").name("Grass Energy")
                .energyType(EnergyType.GRASS)
                .rules(List.of("Provides Grass energy"))
                .build();

        assertThat(card.getEnergyType()).isEqualTo(EnergyType.GRASS);
        assertThat(card.getRules()).containsExactly("Provides Grass energy");
    }

    @Test
    void getSupertype_ReturnsEnergy() {
        EnergyCard card = EnergyCard.builder().id("e1").name("Fire Energy").build();
        assertThat(card.getSupertype()).isEqualTo(Supertype.ENERGY);
    }

    @Test
    void canBePlayed_ReturnsFalse() {
        EnergyCard card = EnergyCard.builder().id("e1").name("Water Energy").build();
        assertThat(card.canBePlayed(null)).isFalse();
    }

    @Test
    void builder_MinimalFields() {
        EnergyCard card = EnergyCard.builder().id("e1").name("Electric Energy").build();
        assertThat(card.getId()).isEqualTo("e1");
        assertThat(card.getName()).isEqualTo("Electric Energy");
        assertThat(card.getEnergyType()).isNull();
        assertThat(card.getRules()).isNull();
    }

    @Test
    void builder_WithAllEnergyTypes() {
        for (EnergyType type : EnergyType.values()) {
            EnergyCard card = EnergyCard.builder()
                    .id("e-" + type.name()).name(type.name() + " Energy")
                    .energyType(type)
                    .build();
            assertThat(card.getEnergyType()).isEqualTo(type);
        }
    }
}
