package ar.edu.utn.frc.tup.piii.dtos.payment.mp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Subconjunto de la respuesta de {@code POST /checkout/preferences} de MercadoPago.
 *
 * <p>Solo se mapean los campos necesarios para continuar el flujo:
 * <ul>
 *   <li>{@code id} — ID de la preferencia, almacenado en {@link ar.edu.utn.frc.tup.piii.models.Purchase}
 *       para correlacionar notificaciones IPN.</li>
 *   <li>{@code initPoint} — URL real de pago a la que el frontend redirige al comprador.</li>
 * </ul>
 */
@Data
@NoArgsConstructor
public class MercadoPagoPreferenceResponse {

    /** ID de la preferencia de pago generado por MercadoPago. */
    private String id;

    /**
     * URL de la pantalla de pago real de MercadoPago (producción).
     * Corresponde al campo {@code init_point} de la respuesta JSON de MP.
     */
    @JsonProperty("init_point")
    private String initPoint;

    /**
     * URL de la pantalla de pago sandbox de MercadoPago (pruebas).
     * Corresponde al campo {@code sandbox_init_point} de la respuesta JSON de MP.
     */
    @JsonProperty("sandbox_init_point")
    private String sandboxInitPoint;
}
