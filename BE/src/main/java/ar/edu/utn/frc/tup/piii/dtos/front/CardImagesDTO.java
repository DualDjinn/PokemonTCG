package ar.edu.utn.frc.tup.piii.dtos.front;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Image URLs for a single Pokémon TCG card.
 */
@Getter
@Setter
@NoArgsConstructor
public class CardImagesDTO {

    /** Low-resolution card image URL. */
    private String small;

    /** High-resolution card image URL. */
    private String large;
}
