package ar.edu.utn.frc.tup.piii.dtos.cardback;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Equipar un dorso de carta como dorso global del jugador.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EquipCardBackRequest {

    /** Id del dorso a equipar; null restaura el dorso clásico por defecto. */
    private String cardBackId;
}
