package ar.edu.utn.frc.tup.piii.dtos.front;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a Pokémon card weakness as returned by the pokemontcg.io API.
 */
@Getter
@Setter
@NoArgsConstructor
public class WeaknessDTO {

    /** Energy type this card is weak to (e.g., "Fire"). */
    private String type;

    /** Weakness multiplier string (e.g., "×2"). */
    private String value;
}
