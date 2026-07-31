package ar.edu.utn.frc.tup.piii.dtos.front;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a Pokémon card ability as returned by the pokemontcg.io API.
 */
@Getter
@Setter
@NoArgsConstructor
public class AbilityDTO {

    /** Ability name (e.g., "Spiky Shield"). */
    private String name;

    /** Full text describing the ability effect. */
    private String text;

    /** Ability type label (typically "Ability"). */
    private String type;
}
