package ar.edu.utn.frc.tup.piii.dtos.front;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents the Pokémon TCG set a card belongs to.
 */
@Getter
@Setter
@NoArgsConstructor
public class SetDTO {

    /** Set identifier (e.g., "xy1"). */
    private String id;

    /** Human-readable set name (e.g., "XY"). */
    private String name;

    /** Series name (e.g., "XY"). */
    private String series;

    /** Total number of cards printed in the set. */
    private int printedTotal;

    /** Total number of cards including secret rares. */
    private int total;

    /** PTCGO code identifier (e.g., "XY"). */
    private String ptcgoCode;

    /** Release date string (e.g., "2014/02/05"). */
    private String releaseDate;

    /** Set symbol and logo image URLs. */
    private SetImagesDTO images;
}
