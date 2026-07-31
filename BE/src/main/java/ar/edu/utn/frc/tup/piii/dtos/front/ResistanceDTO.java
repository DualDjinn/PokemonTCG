package ar.edu.utn.frc.tup.piii.dtos.front;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a Pokémon card resistance as returned by the pokemontcg.io API.
 */
@Getter
@Setter
@NoArgsConstructor
public class ResistanceDTO {

    /** Energy type this card is resistant to (e.g., "Psychic"). */
    private String type;

    /** Resistance modifier string (e.g., "-20"). */
    private String value;
}
