package ar.edu.utn.frc.tup.piii.dtos.skin;

import lombok.Data;

/**
 * Request para equipar una skin en una carta.
 */
@Data
public class EquipSkinRequest {

    private String cardId;
    private String skinId;
}
