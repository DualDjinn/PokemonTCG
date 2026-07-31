package ar.edu.utn.frc.tup.piii.models;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ResistanceTest {

    @Test
    void applyTo_ReducesDamage() {
        Resistance resistance = new Resistance(EnergyType.FIGHTING, 20);
        assertThat(resistance.applyTo(50)).isEqualTo(30);
    }

    @Test
    void applyTo_WhenReductionExceedsDamage_ReturnsZero() {
        Resistance resistance = new Resistance(EnergyType.FIGHTING, 20);
        assertThat(resistance.applyTo(10)).isZero();
    }

    @Test
    void applyTo_WhenDamageEqualsReduction_ReturnsZero() {
        Resistance resistance = new Resistance(EnergyType.FIGHTING, 20);
        assertThat(resistance.applyTo(20)).isZero();
    }

    @Test
    void applyTo_WithNoReduction() {
        Resistance resistance = new Resistance(EnergyType.FIGHTING, 0);
        assertThat(resistance.applyTo(50)).isEqualTo(50);
    }

    @Test
    void getter_Type() {
        Resistance resistance = new Resistance(EnergyType.FIRE, 30);
        assertThat(resistance.getType()).isEqualTo(EnergyType.FIRE);
    }

    @Test
    void getter_Reduction() {
        Resistance resistance = new Resistance(EnergyType.WATER, 20);
        assertThat(resistance.getReduction()).isEqualTo(20);
    }
}
