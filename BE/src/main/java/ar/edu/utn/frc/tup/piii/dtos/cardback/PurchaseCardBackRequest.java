package ar.edu.utn.frc.tup.piii.dtos.cardback;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Compra de un dorso de carta de la tienda.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseCardBackRequest {

    private String cardBackId;
}
