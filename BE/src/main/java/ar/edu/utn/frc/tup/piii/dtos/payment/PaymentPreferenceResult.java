package ar.edu.utn.frc.tup.piii.dtos.payment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Resultado devuelto por {@link ar.edu.utn.frc.tup.piii.services.PaymentGateway#createPreference}.
 *
 * <p>Contiene todo lo que el frontend necesita para redirigir al comprador:
 * <ul>
 *   <li>{@code purchaseId} — ID interno para rastrear la compra en nuestra base de datos.</li>
 *   <li>{@code preferenceId} — ID de la preferencia asignado por MercadoPago.</li>
 *   <li>{@code checkoutUrl} — URL de pago a la que el browser debe redirigir al usuario.</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentPreferenceResult {

    /** ID interno de la compra (UUID generado por el sistema). */
    private String purchaseId;

    /** ID de la preferencia devuelto por la API de MercadoPago. */
    private String preferenceId;

    /** URL de checkout a la que el frontend redirige al comprador. */
    private String checkoutUrl;
}
