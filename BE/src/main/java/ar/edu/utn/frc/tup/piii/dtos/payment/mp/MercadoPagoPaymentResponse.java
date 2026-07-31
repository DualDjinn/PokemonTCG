package ar.edu.utn.frc.tup.piii.dtos.payment.mp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Subconjunto del objeto de pago devuelto por {@code GET /v1/payments/{id}} de MercadoPago.
 *
 * <p>Solo se mapean los campos necesarios para determinar la aprobación o rechazo del pago.
 * Jackson ignora automáticamente todos los demás campos de la respuesta de MP.
 *
 * <p>{@code externalReference} es clave: contiene el {@code purchaseId} interno que
 * se envió como {@code external_reference} al crear la preferencia, permitiendo al
 * handler del webhook correlacionar la notificación con el registro de compra en la BD.
 */
@Data
@NoArgsConstructor
public class MercadoPagoPaymentResponse {

    /** ID numérico del pago en MercadoPago. */
    private Long id;

    /** Estado del pago: {@code "approved"}, {@code "rejected"}, {@code "pending"}, etc. */
    private String status;

    /** Detalle adicional del estado (p.ej. {@code "accredited"}, {@code "cc_rejected_insufficient_amount"}). */
    @JsonProperty("status_detail")
    private String statusDetail;

    /**
     * Referencia externa enviada al crear la preferencia.
     * Contiene el {@code purchaseId} interno para vincular el pago con la compra en la BD.
     */
    @JsonProperty("external_reference")
    private String externalReference;
}
