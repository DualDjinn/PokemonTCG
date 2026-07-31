package ar.edu.utn.frc.tup.piii.dtos.front;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Image URLs for a Pokémon TCG set (symbol and logo).
 */
@Getter
@Setter
@NoArgsConstructor
public class SetImagesDTO {

    /** URL to the set's symbol image. */
    private String symbol;

    /** URL to the set's logo image. */
    private String logo;
}
