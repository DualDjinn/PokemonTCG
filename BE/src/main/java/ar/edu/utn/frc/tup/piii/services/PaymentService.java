package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.payment.CreatePaymentRequest;
import ar.edu.utn.frc.tup.piii.dtos.payment.PaymentNotificationResult;
import ar.edu.utn.frc.tup.piii.dtos.payment.PaymentPreferenceResult;
import ar.edu.utn.frc.tup.piii.dtos.payment.PurchaseStatusResponse;
import ar.edu.utn.frc.tup.piii.models.Purchase;
import java.util.Optional;
import ar.edu.utn.frc.tup.piii.repositories.PlayerRepository;
import ar.edu.utn.frc.tup.piii.repositories.PurchaseRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Servicio de aplicación que orquesta el ciclo de vida completo de un pago.
 *
 * <h2>Rol en el patrón Adapter</h2>
 * <p>Este servicio es el <b>Cliente</b> del patrón: depende exclusivamente de la
 * interfaz {@link PaymentGateway} (el <em>puerto</em>), nunca de
 * {@link ar.edu.utn.frc.tup.piii.services.adapters.MercadoPagoAdapter} directamente.
 * Para reemplazar MercadoPago por otro proveedor (p.ej. PayPal, Stripe) basta con
 * registrar una nueva implementación de {@link PaymentGateway}; este servicio no
 * requiere ningún cambio.
 *
 * <h2>Flujo de pago</h2>
 * <ol>
 *   <li>{@link #createCheckout} — el frontend llama a este método con el carrito.
 *       Se persiste un {@link Purchase} con estado inicial (no aprobado), se delega
 *       la creación de la preferencia al adaptador y se devuelve la URL de checkout.</li>
 *   <li>{@link #handleWebhook} — MercadoPago notifica el resultado del pago vía IPN.
 *       Se actualiza el estado del {@link Purchase} y, si fue aprobado, se acreditan
 *       las monedas al jugador.</li>
 * </ol>
 */
@Service
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    /**
     * Puerto del patrón Adapter: referencia a la interfaz, no a la implementación concreta.
     * Spring inyecta automáticamente el bean que implementa {@link PaymentGateway}
     * (actualmente {@link ar.edu.utn.frc.tup.piii.services.adapters.MercadoPagoAdapter}).
     */
    private final PaymentGateway paymentGateway;
    private final PurchaseRepository purchaseRepository;
    private final PlayerRepository playerRepository;
    private final ObjectMapper objectMapper;
    private final IdempotencyService idempotencyService;

    /**
     * @param paymentGateway     active payment gateway adapter
     * @param purchaseRepository purchase repository used to persist the payment state
     * @param playerRepository   player repository used to credit coins
     * @param objectMapper       JSON serializer used to store the purchased items
     * @param idempotencyService deduplicates the checkout on double submit with the same key
     */
    public PaymentService(PaymentGateway paymentGateway,
            PurchaseRepository purchaseRepository,
            PlayerRepository playerRepository,
            ObjectMapper objectMapper,
            IdempotencyService idempotencyService) {
        this.paymentGateway = paymentGateway;
        this.purchaseRepository = purchaseRepository;
        this.playerRepository = playerRepository;
        this.objectMapper = objectMapper;
        this.idempotencyService = idempotencyService;
    }

    /**
     * Starts the purchase process and returns the checkout URL.
     *
     * <p>Creates and persists a {@link Purchase} with an initial state ({@code approved=false,
     * rejected=false}), calls the adapter to create the preference in the gateway and
     * updates the record with the {@code preferenceId} returned by the provider.
     *
     * <p>It is idempotent with respect to the {@code Idempotency-Key} header: a double
     * submit with the same key reuses the same {@link Purchase}/preference instead of
     * creating duplicates, and the key is forwarded to MercadoPago as {@code X-Idempotency-Key}.
     *
     * @param request        cart data received from the frontend
     * @param idempotencyKey {@code Idempotency-Key} header value; null = no deduplication
     * @return checkout URL and reference IDs for the frontend
     */
    public PaymentPreferenceResult createCheckout(CreatePaymentRequest request, String idempotencyKey) {
        return idempotencyService.execute(idempotencyKey, request.getPlayerId(), "payment:checkout",
                PaymentPreferenceResult.class, () -> doCreateCheckout(request, idempotencyKey));
    }

    private PaymentPreferenceResult doCreateCheckout(CreatePaymentRequest request, String idempotencyKey) {
        String purchaseId = UUID.randomUUID().toString();
        BigDecimal total = request.getItems().stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Purchase purchase = new Purchase();
        purchase.setId(purchaseId);
        purchase.setPlayerId(request.getPlayerId());
        purchase.setTotalAmount(total);
        purchase.setApproved(false);
        purchase.setRejected(false);
        purchase.setCoinsToAdd(calculateCoinsForAmount(total));
        purchase.setCreatedAt(LocalDateTime.now());

        try {
            purchase.setItemsJson(objectMapper.writeValueAsString(request.getItems()));
        } catch (JsonProcessingException e) {
            logger.warn("Could not serialize items for purchase {}: {}", purchaseId, e.getMessage());
        }

        purchaseRepository.save(purchase);

        // Delega la creación de la preferencia al adaptador activo (patrón Adapter)
        PaymentPreferenceResult result = paymentGateway.createPreference(request, purchaseId, idempotencyKey);

        purchase.setPreferenceId(result.getPreferenceId());
        purchaseRepository.save(purchase);

        logger.info("Checkout created: purchaseId={}, preferenceId={}", purchaseId, result.getPreferenceId());
        return result;
    }

    /**
     * Procesa una notificación IPN de MercadoPago y actualiza el estado del pago.
     *
     * <p>Consulta al adaptador el estado final del pago, actualiza el {@link Purchase}
     * en la base de datos y, si fue aprobado, acredita las monedas al jugador.
     *
     * @param mpPaymentId ID del pago en MercadoPago, recibido en el webhook
     */
    @Transactional
    public void handleWebhook(String mpPaymentId) {
        logger.info("Processing webhook for mpPaymentId={}", mpPaymentId);

        // Delega la consulta del estado del pago al adaptador (patrón Adapter)
        PaymentNotificationResult result = paymentGateway.processPayment(mpPaymentId);

        if (result.getPurchaseId() != null) {
            // Lock pesimista para serializar reentregas concurrentes del IPN (BQ-01)
            purchaseRepository.findByIdForUpdate(result.getPurchaseId())
                    .ifPresent(purchase -> applyPaymentResult(purchase, mpPaymentId, result));
        }
    }

    /**
     * Pull-based confirmation triggered by the success page after the MercadoPago redirect.
     *
     * <p>Fetches the payment status directly from MercadoPago (same query the webhook does) and
     * credits the coins, so crediting does not depend on the webhook actually reaching the
     * backend (which is unreliable in local/dev setups behind a tunnel). It is idempotent via
     * the {@code coinsCredited} flag, so it is safe even if the webhook also arrives later.
     *
     * @param mpPaymentId MercadoPago payment id received in the success redirect ({@code payment_id})
     * @param playerId    authenticated player; must own the purchase being confirmed
     * @return the updated purchase status, or empty if the payment has no associated purchase
     * @throws ResponseStatusException 403 if the purchase belongs to a different player
     */
    @Transactional
    public Optional<PurchaseStatusResponse> confirmPayment(String mpPaymentId, String playerId) {
        PaymentNotificationResult result = paymentGateway.processPayment(mpPaymentId);
        if (result.getPurchaseId() == null) {
            return Optional.empty();
        }
        return purchaseRepository.findByIdForUpdate(result.getPurchaseId()).map(purchase -> {
            if (!purchase.getPlayerId().equals(playerId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Purchase does not belong to player");
            }
            applyPaymentResult(purchase, mpPaymentId, result);
            return new PurchaseStatusResponse(purchase.isApproved(), purchase.isRejected(), purchase.getCoinsToAdd());
        });
    }

    /**
     * Applies a MercadoPago payment result to a purchase: updates its status and credits coins
     * once if approved. Shared by the webhook (push) and the success-page confirmation (pull).
     * Idempotent: coins are credited only if {@code !coinsCredited}.
     */
    private void applyPaymentResult(Purchase purchase, String mpPaymentId, PaymentNotificationResult result) {
        purchase.setMpPaymentId(mpPaymentId);
        purchase.setMpStatus(result.getMpStatus());
        purchase.setApproved(result.isApproved());
        purchase.setRejected(result.isRejected());
        purchase.setUpdatedAt(LocalDateTime.now());

        // Idempotencia: solo acredita si está aprobada y aún no se acreditó
        if (result.isApproved() && !purchase.isCoinsCredited() && purchase.getCoinsToAdd() >= 0) {
            creditCoins(purchase.getPlayerId(), purchase.getCoinsToAdd());
            purchase.setCoinsCredited(true);
        }
        purchaseRepository.save(purchase);
        logger.info("Purchase {} updated: approved={}, rejected={}, coinsCredited={}",
                purchase.getId(), result.isApproved(), result.isRejected(), purchase.isCoinsCredited());
    }

    /**
     * Devuelve el estado actual de una compra para que el frontend haga polling
     * hasta que el webhook de MercadoPago confirme la aprobación.
     *
     * @param purchaseId UUID interno de la compra
     * @return estado de la compra, o vacío si no existe
     */
    public Optional<PurchaseStatusResponse> getPurchaseStatus(String purchaseId) {
        return purchaseRepository.findById(purchaseId)
                .map(p -> new PurchaseStatusResponse(p.isApproved(), p.isRejected(), p.getCoinsToAdd()));
    }

    /**
     * Calcula las monedas a acreditar según el monto total pagado.
     * Mapeo fijo: $1→100, $2→500, $3→1000, $4→5000 coins.
     *
     * @param amount monto total de la compra
     * @return cantidad de monedas correspondiente, o 0 si el monto no coincide con ningún paquete
     */
    private int calculateCoinsForAmount(BigDecimal amount) {
        int pesos = amount.setScale(0, RoundingMode.HALF_UP).intValue();
        return switch (pesos) {
            case 1 -> 100;
            case 2 -> 500;
            case 3 -> 1000;
            case 4 -> 5000;
            default -> 0;
        };
    }

    /**
     * Acredita monedas al jugador tras un pago aprobado.
     *
     * @param playerId ID del jugador al que se le suman las monedas
     * @param coins    cantidad de monedas a acreditar
     */
    private void creditCoins(String playerId, int coins) {
        playerRepository.findById(playerId).ifPresent(player -> {
            player.setCoins(player.getCoins() + coins);
            playerRepository.save(player);
            logger.info("Credited {} coins to player {}", coins, playerId);
        });
    }
}
