package ar.edu.utn.frc.tup.piii.dtos;

import lombok.Getter;

/**
 * DTO for image URLs returned by the pokemontcg.io API.
 *
 * <p>This class is reused for two different image objects in the API response:
 * <ul>
 *   <li><b>Card images</b> — populated via {@code small} and {@code large}.</li>
 *   <li><b>Set images</b> (inside {@code SetDTO}) — populated via {@code symbol} and {@code logo}.</li>
 * </ul>
 * Jackson fills only the fields present in each JSON object; the others remain null.
 */
@Getter
public class ImagesDTO {
    /** Thumbnail URL for a card image, e.g. {@code "https://images.pokemontcg.io/xy1/1.png"}. */
    private String small;

    /** High-resolution URL for a card image, e.g. {@code "https://images.pokemontcg.io/xy1/1_hires.png"}. */
    private String large;

    /** Symbol URL for a set image, e.g. {@code "https://images.pokemontcg.io/xy1/symbol.png"}. */
    private String symbol;

    /** Logo URL for a set image, e.g. {@code "https://images.pokemontcg.io/xy1/logo.png"}. */
    private String logo;
}
