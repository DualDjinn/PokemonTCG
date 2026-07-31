package ar.edu.utn.frc.tup.piii.models;

import ar.edu.utn.frc.tup.piii.models.converters.EnergyTypeCostMapConverter;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "attack")
public class Attack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    @Convert(converter = EnergyTypeCostMapConverter.class)
    @Column(name = "energy_cost", columnDefinition = "TEXT")
    private Map<EnergyType, Integer> energyCost;
    @Column(name = "base_damage")
    private int baseDamage;
    @Enumerated(EnumType.STRING)
    @Column(name = "damage_modifier")
    private DamageModifier damageModifier;
    @Column(name = "effect_text", columnDefinition = "TEXT")
    private String effectText;
    @Transient
    private AttackEffect effect;

    public String getName() { return name; }

    public Map<EnergyType, Integer> getEnergyCost() { return energyCost; }

    public int getBaseDamage() { return baseDamage; }

    public DamageModifier getDamageModifier() { return damageModifier; }

    public String getEffectText() { return effectText; }

    public AttackEffect getEffect() { return effect; }

    public int getTotalEnergyCost() {
        return energyCost.values().stream().mapToInt(Integer::intValue).sum();
    }
}
