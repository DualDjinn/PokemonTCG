package ar.edu.utn.frc.tup.piii.dtos.payment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Estado actual de una compra, usado por el frontend para hacer polling
 * hasta que el webhook de MercadoPago confirme la aprobación del pago.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PurchaseStatusResponse {

    /** true cuando MercadoPago confirmó el pago vía webhook. */
    private boolean approved;

    /** true cuando MercadoPago rechazó el pago vía webhook. */
    private boolean rejected;

    /** Coins que se acreditarán (o ya se acreditaron) al jugador. */
    private int coinsToAdd;
}
