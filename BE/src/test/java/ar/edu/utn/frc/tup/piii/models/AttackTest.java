package ar.edu.utn.frc.tup.piii.models;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class AttackTest {

    @Test
    void getTotalEnergyCost_ReturnsSum() {
        Attack attack = Attack.builder()
                .name("Test Attack")
                .energyCost(Map.of(
                        EnergyType.GRASS, 2,
                        EnergyType.COLORLESS, 1
                ))
                .baseDamage(30)
                .damageModifier(DamageModifier.NONE)
                .build();
        assertThat(attack.getTotalEnergyCost()).isEqualTo(3);
    }

    @Test
    void getTotalEnergyCost_WhenEmpty_ReturnsZero() {
        Attack attack = Attack.builder()
                .name("Empty Attack")
                .energyCost(Map.of())
                .baseDamage(10)
                .damageModifier(DamageModifier.NONE)
                .build();
        assertThat(attack.getTotalEnergyCost()).isZero();
    }

    @Test
    void getTotalEnergyCost_WithSingleType() {
        Attack attack = Attack.builder()
                .name("Fire Blast")
                .energyCost(Map.of(EnergyType.FIRE, 4))
                .baseDamage(80)
                .damageModifier(DamageModifier.NONE)
                .build();
        assertThat(attack.getTotalEnergyCost()).isEqualTo(4);
    }

    @Test
    void getter_Name() {
        Attack attack = Attack.builder()
                .name("Thunderbolt")
                .energyCost(Map.of(EnergyType.LIGHTNING, 2))
                .baseDamage(50)
                .damageModifier(DamageModifier.PLUS)
                .effectText("Flip a coin. If heads, the Defending Pokémon is now Paralyzed.")
                .build();
        assertThat(attack.getName()).isEqualTo("Thunderbolt");
    }

    @Test
    void getter_EnergyCost() {
        Attack attack = Attack.builder()
                .name("Fire Spin")
                .energyCost(Map.of(EnergyType.FIRE, 3))
                .baseDamage(100)
                .damageModifier(DamageModifier.NONE)
                .build();
        assertThat(attack.getEnergyCost()).containsEntry(EnergyType.FIRE, 3);
    }

    @Test
    void getter_BaseDamage() {
        Attack attack = Attack.builder()
                .name("Vine Whip")
                .energyCost(Map.of(EnergyType.GRASS, 1))
                .baseDamage(30)
                .damageModifier(DamageModifier.NONE)
                .build();
        assertThat(attack.getBaseDamage()).isEqualTo(30);
    }

    @Test
    void getter_DamageModifier() {
        Attack attack = Attack.builder()
                .name("Tackle Plus")
                .energyCost(Map.of(EnergyType.COLORLESS, 2))
                .baseDamage(20)
                .damageModifier(DamageModifier.PLUS)
                .build();
        assertThat(attack.getDamageModifier()).isEqualTo(DamageModifier.PLUS);
    }

    @Test
    void getter_EffectText() {
        Attack attack = Attack.builder()
                .name("Confuse Ray")
                .energyCost(Map.of(EnergyType.PSYCHIC, 2))
                .baseDamage(30)
                .damageModifier(DamageModifier.NONE)
                .effectText("The Defending Pokémon is now Confused.")
                .build();
        assertThat(attack.getEffectText()).isEqualTo("The Defending Pokémon is now Confused.");
    }

    @Test
    void getter_Effect() {
        AttackEffect effect = ctx -> ctx.setPendingDamage(999);
        Attack attack = Attack.builder()
                .name("Special Attack")
                .energyCost(Map.of(EnergyType.FIRE, 2))
                .baseDamage(40)
                .damageModifier(DamageModifier.NONE)
                .effect(effect)
                .build();
        assertThat(attack.getEffect()).isSameAs(effect);
    }
}
