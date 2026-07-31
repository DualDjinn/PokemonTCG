package ar.edu.utn.frc.tup.piii.dtos.skin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Skin de carta para respuestas del catálogo.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardSkinDTO {

    private String id;
    private String name;
    private String baseCardId;
    private String origin;
    private String imageUrl;
    private String imageUrlLarge;
    private String tag;
}
