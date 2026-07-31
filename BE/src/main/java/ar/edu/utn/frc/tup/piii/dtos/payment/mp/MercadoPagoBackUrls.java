package ar.edu.utn.frc.tup.piii.dtos.payment.mp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * URLs de redirección del browser enviadas en el request de preferencia a MercadoPago.
 *
 * <p>Tras el proceso de pago, MP redirige el browser del comprador a la URL
 * correspondiente al estado final:
 * <ul>
 *   <li>{@code success} — pago aprobado.</li>
 *   <li>{@code failure} — pago rechazado.</li>
 *   <li>{@code pending} — pago pendiente (transferencias, efectivo, etc.).</li>
 * </ul>
 *
 * <p><b>Nota:</b> en entornos locales, estas URLs pueden apuntar a {@code localhost}
 * porque el browser del usuario corre en la misma máquina. Sin embargo, cuando
 * {@code auto_return} está activo, MP valida que {@code success} sea una URL pública,
 * por lo que {@code auto_return} se omite en desarrollo.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MercadoPagoBackUrls {

    /** URL a la que MP redirige tras un pago aprobado. */
    private String success;

    /** URL a la que MP redirige si el pago es rechazado. */
    private String failure;

    /** URL a la que MP redirige si el pago queda pendiente de acreditación. */
    private String pending;
}
