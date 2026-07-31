package ar.edu.utn.frc.tup.piii.dtos;

import lombok.Getter;

/**
 * DTO representing a Pokémon's resistance as returned by the pokemontcg.io API.
 * Example: type="Psychic", value="-20".
 */
@Getter
public class ResistanceDTO {
    private String type;
    private String value;
}
