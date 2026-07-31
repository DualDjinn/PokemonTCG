package ar.edu.utn.frc.tup.piii.dtos.payment.mp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * Forma específica de ítem que espera la API de MercadoPago en {@code POST /checkout/preferences}.
 *
 * <p>Los campos usan snake_case mediante {@code @JsonProperty} para cumplir con el contrato
 * REST de MP, mientras que los getters/setters de Lombok siguen la convención Java (camelCase).
 *
 * <p>Este DTO es exclusivo del adaptador: solo
 * {@link ar.edu.utn.frc.tup.piii.services.adapters.MercadoPagoAdapter} lo instancia,
 * traduciendo desde el DTO agnóstico
 * {@link ar.edu.utn.frc.tup.piii.dtos.payment.PaymentItemDto}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MercadoPagoItem {

    /** Identificador del ítem (opcional en MP, pero recomendado para trazabilidad). */
    private String id;

    /** Nombre del ítem que se muestra en la pantalla de pago de MP. */
    private String title;

    /** Cantidad comprada. */
    private int quantity;

    /** Precio unitario; MP espera el campo como {@code unit_price} en el JSON. */
    @JsonProperty("unit_price")
    private BigDecimal unitPrice;

    /** Código de moneda ISO 4217 (p.ej. {@code "ARS"} para pesos argentinos). */
    @JsonProperty("currency_id")
    private String currencyId;
}
