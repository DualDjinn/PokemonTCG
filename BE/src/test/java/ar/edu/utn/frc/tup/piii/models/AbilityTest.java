package ar.edu.utn.frc.tup.piii.models;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AbilityTest {

    @Test
    void constructor_NoArgs_InitializesFieldsToDefaults() {
        Ability ability = new Ability();
        assertThat(ability.getId()).isNull();
        assertThat(ability.getName()).isNull();
        assertThat(ability.getDescription()).isNull();
        assertThat(ability.getType()).isNull();
    }

    @Test
    void constructor_Parameterized_SetsFields() {
        Ability ability = new Ability("Adaptability", "Boosts same-type attacks", AbilityType.PASSIVE);
        assertThat(ability.getName()).isEqualTo("Adaptability");
        assertThat(ability.getDescription()).isEqualTo("Boosts same-type attacks");
        assertThat(ability.getType()).isEqualTo(AbilityType.PASSIVE);
    }

    @Test
    void constructor_WithActivatedType() {
        Ability ability = new Ability("Conversion", "Change type once per turn", AbilityType.ACTIVATED);
        assertThat(ability.getName()).isEqualTo("Conversion");
        assertThat(ability.getType()).isEqualTo(AbilityType.ACTIVATED);
    }

    @Test
    void constructor_WithTriggeredType() {
        Ability ability = new Ability("Rough Skin", "Damages attacker on contact", AbilityType.TRIGGERED);
        assertThat(ability.getName()).isEqualTo("Rough Skin");
        assertThat(ability.getType()).isEqualTo(AbilityType.TRIGGERED);
    }
}
