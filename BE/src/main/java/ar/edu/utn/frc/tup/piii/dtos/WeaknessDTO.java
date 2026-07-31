package ar.edu.utn.frc.tup.piii.dtos;

import lombok.Getter;

/**
 * DTO representing a Pokémon's weakness as returned by the pokemontcg.io API.
 * Example: type="Fire", value="×2".
 */
@Getter
public class WeaknessDTO {
    private String type;
    private String value;
}
