package ar.edu.utn.frc.tup.piii.dtos.payment;

import lombok.Data;
import java.util.List;

/**
 * DTO del body de {@code POST /api/payments/create}.
 *
 * <p>Transporta la identidad del comprador, la lista de ítems a pagar y
 * la cantidad de monedas a acreditar si el pago es aprobado.
 * Es agnóstico al proveedor: el adaptador activo ({@link ar.edu.utn.frc.tup.piii.services.PaymentGateway})
 * lo traduce al formato propio de cada pasarela.
 */
@Data
public class CreatePaymentRequest {

    /** ID interno del jugador que inicia la compra. */
    private String playerId;

    /** Lista de ítems del carrito a incluir en la preferencia de pago. */
    private List<PaymentItemDto> items;

    /** Monedas a acreditar al jugador si el pago resulta aprobado. */
    private int coinsToAdd;
}
