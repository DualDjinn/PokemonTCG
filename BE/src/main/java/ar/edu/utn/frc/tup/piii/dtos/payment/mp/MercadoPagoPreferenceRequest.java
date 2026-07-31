package ar.edu.utn.frc.tup.piii.dtos.payment.mp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * Body del request para {@code POST /checkout/preferences} de la API de MercadoPago.
 *
 * <p>{@code @JsonInclude(NON_NULL)} garantiza que los campos opcionales como
 * {@code notificationUrl} y {@code autoReturn} se omitan del JSON cuando son {@code null}.
 * Esto es necesario porque la API de producción de MP rechaza URLs de localhost en esos
 * campos; el adaptador los deja en {@code null} cuando el entorno es local.
 *
 * <p>Todos los campos usan {@code @JsonProperty} para respetar el snake_case
 * que exige el contrato REST de MercadoPago.
 */
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MercadoPagoPreferenceRequest {

    /** Lista de ítems que se mostrarán en la pantalla de pago de MP. */
    private List<MercadoPagoItem> items;

    /** URLs de redirección del browser tras el pago (éxito, fallo, pendiente). */
    @JsonProperty("back_urls")
    private MercadoPagoBackUrls backUrls;

    /**
     * Si es {@code "approved"}, MP redirige automáticamente al comprador a
     * {@code back_urls.success} tras un pago exitoso.
     * Se omite en entornos locales porque MP requiere que {@code back_urls.success}
     * sea una URL pública cuando este campo está presente.
     */
    @JsonProperty("auto_return")
    private String autoReturn;

    /**
     * URL pública donde MP enviará notificaciones IPN (webhook).
     * Se omite en entornos locales porque MP rechaza URLs de localhost en producción.
     */
    @JsonProperty("notification_url")
    private String notificationUrl;

    /**
     * Referencia interna enviada a MP y devuelta en el webhook.
     * Contiene el {@code purchaseId} para vincular la notificación con la compra en la BD.
     */
    @JsonProperty("external_reference")
    private String externalReference;
}
