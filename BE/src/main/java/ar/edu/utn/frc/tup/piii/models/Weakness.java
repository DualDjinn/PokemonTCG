package ar.edu.utn.frc.tup.piii.models;

import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class Weakness {

    private EnergyType type;
    private String value;

    public Weakness(EnergyType type, String value) {
        this.type = type;
        this.value = value;
    }

    public int applyTo(int damage) {
        int multiplier = Integer.parseInt(value.replaceAll("[^0-9]", ""));
        return damage * multiplier;
    }
}
