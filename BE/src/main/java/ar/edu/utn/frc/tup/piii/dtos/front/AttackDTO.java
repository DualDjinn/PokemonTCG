package ar.edu.utn.frc.tup.piii.dtos.front;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a Pokémon card attack as returned by the pokemontcg.io API.
 */
@Getter
@Setter
@NoArgsConstructor
public class AttackDTO {

    /** Attack name (e.g., "Jungle Hammer"). */
    private String name;

    /** Energy cost list (e.g., ["Grass", "Colorless", "Colorless"]). */
    private List<String> cost;

    /** Total energy cost as a single integer. */
    private Integer convertedEnergyCost;

    /** Damage string (e.g., "90", "30+", "20×"). */
    private String damage;

    /** Attack effect description text. */
    private String text;
}
