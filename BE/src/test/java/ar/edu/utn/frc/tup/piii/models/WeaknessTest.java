package ar.edu.utn.frc.tup.piii.models;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class WeaknessTest {

    @Test
    void applyTo_MultipliesDamage() {
        Weakness weakness = new Weakness(EnergyType.FIRE, "×2");
        assertThat(weakness.applyTo(30)).isEqualTo(60);
    }

    @Test
    void applyTo_WithDifferentMultiplier() {
        Weakness weakness = new Weakness(EnergyType.WATER, "×2");
        assertThat(weakness.applyTo(50)).isEqualTo(100);
    }

    @Test
    void getter_Type() {
        Weakness weakness = new Weakness(EnergyType.PSYCHIC, "×2");
        assertThat(weakness.getType()).isEqualTo(EnergyType.PSYCHIC);
    }

    @Test
    void getter_Value() {
        Weakness weakness = new Weakness(EnergyType.FIGHTING, "×2");
        assertThat(weakness.getValue()).isEqualTo("×2");
    }
}
