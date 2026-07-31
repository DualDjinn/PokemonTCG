package ar.edu.utn.frc.tup.piii.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Represents the image URLs for a single Pokémon TCG card.
 * The {@code small} URL points to a low-resolution preview and
 * the {@code large} URL points to the high-resolution scan.
 */
@Getter
@NoArgsConstructor
public class CardImagesDTO {

    /** Low-resolution card image URL. */
    private String small;

    /** High-resolution card image URL. */
    private String large;
}
