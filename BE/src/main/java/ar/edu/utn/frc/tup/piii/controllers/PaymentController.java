package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.configs.Authenticated;
import ar.edu.utn.frc.tup.piii.configs.PaymentConfig;
import ar.edu.utn.frc.tup.piii.dtos.payment.CreatePaymentRequest;
import ar.edu.utn.frc.tup.piii.dtos.payment.PaymentPreferenceResult;
import ar.edu.utn.frc.tup.piii.dtos.payment.PurchaseStatusResponse;
import ar.edu.utn.frc.tup.piii.models.Player;
import ar.edu.utn.frc.tup.piii.services.PaymentService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * REST controller que expone los endpoints del flujo de pago.
 *
 * <p>Capa delgada: no contiene lógica de negocio, solo delega a {@link PaymentService}.
 * Cubre el ciclo de vida completo de Checkout Pro con dos endpoints:
 * <ul>
 *   <li>{@code POST /api/payments/create} — el frontend llama a este endpoint para
 *       iniciar un pago; devuelve la URL de redirección a MercadoPago.</li>
 *   <li>{@code POST /api/payments/webhook} — MercadoPago llama a este endpoint vía
 *       webhook v2 (body JSON + {@code x-signature}) o IPN legacy
 *       ({@code ?topic=payment&id=xxx}, sin body ni firma).</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;
    private final PaymentConfig paymentConfig;
    private final ObjectMapper objectMapper;

    /**
     * @param paymentService servicio que orquesta el ciclo de vida del pago
     * @param paymentConfig  configuración con el webhook secret para validar firmas
     * @param objectMapper   deserializador JSON para leer el body del webhook v2
     */
    public PaymentController(PaymentService paymentService,
            PaymentConfig paymentConfig,
            ObjectMapper objectMapper) {
        this.paymentService = paymentService;
        this.paymentConfig = paymentConfig;
        this.objectMapper = objectMapper;
    }

    /**
     * Inicia un flujo de Checkout Pro.
     *
     * <p>Crea un registro {@code Purchase}, delega la creación de la preferencia al
     * adaptador activo de {@link ar.edu.utn.frc.tup.piii.services.PaymentGateway} y
     * devuelve la URL de checkout para que el navegador redirija al comprador.
     *
     * @param request ítems del carrito, ID del jugador y coins a acreditar al aprobar
     * @return resultado con la URL de checkout y los IDs de preferencia y compra
     */
    @PostMapping("/create")
    public ResponseEntity<PaymentPreferenceResult> createPayment(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Authenticated Player player,
            @RequestBody CreatePaymentRequest request) {
        // The buyer identity comes from the session, not the body (which is tamperable).
        request.setPlayerId(player.getId());
        return ResponseEntity.ok(paymentService.createCheckout(request, idempotencyKey));
    }

    /**
     * Devuelve el estado de una compra para que el frontend haga polling
     * hasta confirmar que el webhook fue procesado.
     *
     * @param purchaseId UUID interno de la compra (enviado como external_reference a MP)
     * @return estado de la compra o 404 si no existe
     */
    @GetMapping("/{purchaseId}/status")
    public ResponseEntity<PurchaseStatusResponse> getPurchaseStatus(
            @PathVariable String purchaseId) {
        return paymentService.getPurchaseStatus(purchaseId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Confirms a payment after the MercadoPago redirect (pull-based), crediting the coins
     * without depending on the webhook reaching the backend. The {@code paymentId} comes from
     * the success URL ({@code payment_id}).
     *
     * @param player    authenticated buyer (must own the purchase)
     * @param paymentId MercadoPago payment id from the success redirect
     * @return the updated purchase status, or 404 if the payment has no associated purchase
     */
    @PostMapping("/confirm")
    public ResponseEntity<PurchaseStatusResponse> confirmPayment(
            @Authenticated Player player,
            @RequestParam("paymentId") String paymentId) {
        return paymentService.confirmPayment(paymentId, player.getId())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Recibe notificaciones de MercadoPago en dos formatos posibles:
     *
     * <ul>
     *   <li><b>Webhook v2</b> (formato actual de MP): {@code POST /webhook?id={notifId}&topic=payment}
     *       con body JSON {@code {"data":{"id":"{paymentId}"},...}} y header {@code x-signature}.
     *       El ID del pago real viene en {@code data.id} del body; el {@code id} del query param
     *       es el ID de notificación (distinto). La firma HMAC-SHA256 también usa {@code data.id}.</li>
     *   <li><b>IPN legacy</b>: {@code POST /webhook?topic=payment&id={paymentId}} sin body
     *       ni {@code x-signature}. El ID del pago viene directamente en el query param.</li>
     * </ul>
     *
     * @param notificationId ID de notificación del query param (usado solo en IPN)
     * @param topic          tópico; solo {@code "payment"} es procesado
     * @param xSignature     header {@code x-signature} de webhook v2 ({@code ts=...,v1=...})
     * @param xRequestId     header {@code x-request-id} de webhook v2
     * @param body           body JSON del webhook v2; null en IPN
     */
    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(
            @RequestParam(value = "id", required = false) String notificationId,
            @RequestParam(value = "topic", required = false) String topic,
            @RequestHeader(value = "x-signature", required = false) String xSignature,
            @RequestHeader(value = "x-request-id", required = false) String xRequestId,
            @RequestBody(required = false) String body) {

        // Webhook v2 sends the real payment ID inside data.id of the JSON body.
        // IPN sends it directly in the "id" query param.
        String paymentId = extractPaymentId(notificationId, xSignature, body);

        logger.info("Webhook received — topic={}, paymentId={}, notificationId={}, xSignature={}",
                topic, paymentId, notificationId, xSignature != null ? "present" : "absent");

        String secret = paymentConfig.getWebhookSecret();
        if (secret != null && !secret.isBlank() && xSignature != null) {
            if (!isValidSignature(paymentId, xRequestId, xSignature, secret)) {
                logger.warn("Rejected webhook with invalid signature for paymentId={}", paymentId);
                return ResponseEntity.status(403).build();
            }
        }

        if ("payment".equals(topic) && paymentId != null) {
            paymentService.handleWebhook(paymentId);
        }
        return ResponseEntity.ok().build();
    }

    /**
     * Extrae el ID del pago según el formato del webhook recibido.
     *
     * <p>Webhook v2: el ID real del pago está en {@code data.id} del body JSON.
     * IPN legacy: el ID del pago es el query param {@code id}.
     *
     * @param notificationId query param {@code id} (ID de notificación en v2, ID de pago en IPN)
     * @param xSignature     header {@code x-signature}; presente solo en webhook v2
     * @param body           body JSON; presente solo en webhook v2
     * @return ID del pago a procesar
     */
    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    private String extractPaymentId(String notificationId, String xSignature, String body) {
        if (xSignature != null && body != null && !body.isBlank()) {
            try {
                JsonNode root = objectMapper.readTree(body);
                JsonNode dataId = root.path("data").path("id");
                if (!dataId.isMissingNode() && !dataId.isNull()) {
                    return dataId.asText();
                }
            } catch (Exception e) {
                logger.warn("Could not parse webhook body to extract data.id: {}", e.getMessage());
            }
        }
        return notificationId;
    }

    /**
     * Valida la firma HMAC-SHA256 del header {@code x-signature} de MercadoPago.
     *
     * <p>MP firma el template {@code id:{paymentId};request-id:{xRequestId};ts:{ts};}
     * donde {@code paymentId} es {@code data.id} del body (no el ID de notificación).
     *
     * @param paymentId  ID del pago extraído de {@code data.id} del body
     * @param xRequestId header {@code x-request-id} de la request
     * @param xSignature header {@code x-signature} con timestamp y hash
     * @param secret     webhook secret configurado en el panel de MP
     * @return {@code true} si el hash calculado coincide con el enviado por MP
     */
    private boolean isValidSignature(String paymentId, String xRequestId, String xSignature, String secret) {
        try {
            String ts = null;
            String v1 = null;
            for (String part : xSignature.split(",")) {
                if (part.startsWith("ts=")) {
                    ts = part.substring(3);
                } else if (part.startsWith("v1=")) {
                    v1 = part.substring(3);
                }
            }
            if (ts == null || v1 == null) {
                return false;
            }

            // x-request-id is optional; MP uses empty string when absent
            String reqId = xRequestId != null ? xRequestId : "";
            String manifest = "id:" + paymentId + ";request-id:" + reqId + ";ts:" + ts + ";";

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(hexToBytes(secret), "HmacSHA256"));
            byte[] hashBytes = mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder();
            for (byte b : hashBytes) {
                hex.append(String.format("%02x", b));
            }

            boolean valid = hex.toString().equals(v1);
            if (!valid) {
                logger.warn("Signature mismatch — computed: {}, received: {}, xRequestId: [{}]",
                        hex, v1, reqId);
            }
            return valid;
        } catch (Exception e) {
            logger.warn("Error validating webhook signature: {}", e.getMessage());
            return false;
        }
    }
}
