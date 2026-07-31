package ar.edu.utn.frc.tup.piii.dtos.payment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Resultado normalizado de {@link ar.edu.utn.frc.tup.piii.services.PaymentGateway#processPayment}.
 *
 * <p>Abstrae el estado de pago de cualquier proveedor en dos flags booleanos simples,
 * evitando que la capa de servicio conozca los valores de estado específicos de MP
 * (como "approved", "rejected", "in_process", etc.).
 *
 * <p>Ambos flags pueden ser {@code false} simultáneamente cuando el pago está pendiente
 * de resolución (p.ej. transferencias bancarias o pagos en efectivo).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentNotificationResult {

    /**
     * ID interno de la compra, recuperado del {@code external_reference} de MP.
     * Permite al servicio ubicar el registro {@link ar.edu.utn.frc.tup.piii.models.Purchase}
     * correspondiente en la base de datos.
     */
    private String purchaseId;

    /** {@code true} si el proveedor confirmó el pago como aprobado. */
    private boolean approved;

    /** {@code true} si el proveedor rechazó el pago. */
    private boolean rejected;

    /** Estado crudo devuelto por el proveedor, para registro y auditoría. */
    private String mpStatus;
}
