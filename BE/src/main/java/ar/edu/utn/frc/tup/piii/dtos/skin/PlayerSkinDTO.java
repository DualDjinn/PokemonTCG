package ar.edu.utn.frc.tup.piii.dtos.skin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Skin que posee un jugador.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerSkinDTO {

    private String skinId;
    private String skinName;
    private String baseCardId;
    private String baseCardName;
    private String origin;
    private String imageUrl;
    private String imageUrlLarge;
    private int quantity;
    /** Si la skin está activa en la carta base del jugador. */
    private boolean equipped;
}
