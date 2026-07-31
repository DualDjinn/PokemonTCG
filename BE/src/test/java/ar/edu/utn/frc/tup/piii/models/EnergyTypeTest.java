package ar.edu.utn.frc.tup.piii.models;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnergyTypeTest {

    @Test
    void values_ContainAllTypes() {
        assertThat(EnergyType.values()).containsExactly(
                EnergyType.GRASS,
                EnergyType.FIRE,
                EnergyType.WATER,
                EnergyType.LIGHTNING,
                EnergyType.PSYCHIC,
                EnergyType.FIGHTING,
                EnergyType.DARKNESS,
                EnergyType.METAL,
                EnergyType.FAIRY,
                EnergyType.DRAGON,
                EnergyType.COLORLESS
        );
    }

    @Test
    void fromJson_WithValidValue_ReturnsCorrectType() {
        assertThat(EnergyType.fromJson("Grass")).isEqualTo(EnergyType.GRASS);
        assertThat(EnergyType.fromJson("Fire")).isEqualTo(EnergyType.FIRE);
        assertThat(EnergyType.fromJson("Water")).isEqualTo(EnergyType.WATER);
        assertThat(EnergyType.fromJson("Lightning")).isEqualTo(EnergyType.LIGHTNING);
        assertThat(EnergyType.fromJson("Psychic")).isEqualTo(EnergyType.PSYCHIC);
        assertThat(EnergyType.fromJson("Fighting")).isEqualTo(EnergyType.FIGHTING);
        assertThat(EnergyType.fromJson("Darkness")).isEqualTo(EnergyType.DARKNESS);
        assertThat(EnergyType.fromJson("Metal")).isEqualTo(EnergyType.METAL);
        assertThat(EnergyType.fromJson("Fairy")).isEqualTo(EnergyType.FAIRY);
        assertThat(EnergyType.fromJson("Dragon")).isEqualTo(EnergyType.DRAGON);
        assertThat(EnergyType.fromJson("Colorless")).isEqualTo(EnergyType.COLORLESS);
    }

    @Test
    void fromJson_WithInvalidValue_ThrowsException() {
        assertThatThrownBy(() -> EnergyType.fromJson("UnknownType"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tipo desconocido");
    }

    @Test
    void fromJson_CaseSensitive() {
        assertThatThrownBy(() -> EnergyType.fromJson("grass"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
