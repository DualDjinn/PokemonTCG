package ar.edu.utn.frc.tup.piii.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad JPA que persiste el estado de una transacción de pago.
 *
 * <p>Ciclo de vida:
 * <ol>
 *   <li>Se crea en {@code PaymentService.createCheckout()} con
 *       {@code approved=false} y {@code rejected=false}.</li>
 *   <li>Se actualiza en {@code PaymentService.handleWebhook()} cuando MercadoPago
 *       envía la notificación IPN con el estado final del pago.</li>
 * </ol>
 *
 * <p>{@code coinsToAdd} se almacena en esta entidad (y no solo en la sesión del browser)
 * para que las monedas puedan acreditarse correctamente incluso si el webhook de MP
 * llega de forma asíncrona, mucho después de que la sesión del comprador haya terminado.
 */
@Data
@Entity
@Table(name = "purchase")
public class Purchase {

    /** UUID generado internamente; también se envía a MP como {@code external_reference}. */
    @Id
    private String id;

    /** ID del jugador que inició la compra. */
    @Column(name = "player_id", nullable = false)
    private String playerId;

    /** ID de la preferencia de pago en MercadoPago, obtenido al crear el checkout. */
    @Column(name = "preference_id")
    private String preferenceId;

    /** ID del pago en MercadoPago, recibido en el webhook IPN. */
    @Column(name = "mp_payment_id")
    private String mpPaymentId;

    /** Estado crudo devuelto por la API de MP (p.ej. "approved", "rejected", "pending"). */
    @Column(name = "mp_status")
    private String mpStatus;

    /** Monto total calculado a partir de los ítems del carrito. */
    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    /** {@code true} si MP confirmó el pago; {@code false} en cualquier otro estado. */
    @Column(nullable = false)
    private boolean approved;

    /** {@code true} si MP rechazó el pago; {@code false} en cualquier otro estado. */
    @Column(nullable = false)
    private boolean rejected;

    /** Fecha y hora en que se creó la preferencia de pago. */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** Fecha y hora de la última actualización de estado vía webhook. */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** Serialización JSON de los ítems comprados, para registro histórico. */
    @Column(name = "items_json", columnDefinition = "TEXT")
    private String itemsJson;

    /** Cantidad de monedas a acreditar al jugador si el pago es aprobado. */
    @Column(name = "coins_to_add", nullable = false)
    private int coinsToAdd;

    /** {@code true} una vez acreditadas las monedas de esta compra; evita re-acreditar ante reentregas del IPN. */
    @Column(name = "coins_credited", nullable = false)
    private boolean coinsCredited;
}
