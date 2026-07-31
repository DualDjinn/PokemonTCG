package ar.edu.utn.frc.tup.piii.models;

import ar.edu.utn.frc.tup.piii.models.converters.StringListConverter;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
@Table(name = "energy_card")
public class EnergyCard extends Card {

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "energy_type")
    private EnergyType energyType;
    @Setter
    @Convert(converter = StringListConverter.class)
    @Column(name = "rules", columnDefinition = "TEXT")
    private List<String> rules;

    @Override
    public Supertype getSupertype() {
        return Supertype.ENERGY;
    }

    @Override
    public boolean canBePlayed(GameContext ctx) {
        return false;
    }
}
