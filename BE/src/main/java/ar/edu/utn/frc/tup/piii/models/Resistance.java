package ar.edu.utn.frc.tup.piii.models;

import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class Resistance {

    private EnergyType type;
    private int reduction;

    public Resistance(EnergyType type, int reduction) {
        this.type = type;
        this.reduction = reduction;
    }

    public int applyTo(int damage) {
        return Math.max(0, damage - reduction);
    }
}
