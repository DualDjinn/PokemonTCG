package ar.edu.utn.frc.tup.piii.dtos.payment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * Representación interna y agnóstica al proveedor de un ítem comprable.
 *
 * <p>En el flujo de pago, este DTO viaja desde el frontend hasta el servicio.
 * Dentro del adaptador (p.ej. {@link ar.edu.utn.frc.tup.piii.services.adapters.MercadoPagoAdapter})
 * se convierte al formato específico del proveedor
 * (p.ej. {@link ar.edu.utn.frc.tup.piii.dtos.payment.mp.MercadoPagoItem}),
 * manteniendo la capa de servicio desacoplada de los nombres y tipos de campo de MP.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentItemDto {

    /** Identificador del ítem (p.ej. "pkg-100-coins"). */
    private String id;

    /** Nombre visible del ítem en la pantalla de pago de MP. */
    private String title;

    /** Cantidad comprada. */
    private int quantity;

    /** Precio unitario en la moneda local (ARS). */
    private BigDecimal unitPrice;
}
