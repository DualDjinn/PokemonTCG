package ar.edu.utn.frc.tup.piii.services.adapters;

import ar.edu.utn.frc.tup.piii.configs.PaymentConfig;
import ar.edu.utn.frc.tup.piii.dtos.payment.CreatePaymentRequest;
import ar.edu.utn.frc.tup.piii.dtos.payment.PaymentItemDto;
import ar.edu.utn.frc.tup.piii.dtos.payment.PaymentNotificationResult;
import ar.edu.utn.frc.tup.piii.dtos.payment.PaymentPreferenceResult;
import ar.edu.utn.frc.tup.piii.dtos.payment.mp.MercadoPagoBackUrls;
import ar.edu.utn.frc.tup.piii.dtos.payment.mp.MercadoPagoItem;
import ar.edu.utn.frc.tup.piii.dtos.payment.mp.MercadoPagoPaymentResponse;
import ar.edu.utn.frc.tup.piii.dtos.payment.mp.MercadoPagoPreferenceRequest;
import ar.edu.utn.frc.tup.piii.dtos.payment.mp.MercadoPagoPreferenceResponse;
import ar.edu.utn.frc.tup.piii.services.PaymentGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import java.util.List;

/**
 * <b>Adaptador</b> de MercadoPago Checkout Pro para la interfaz interna {@link PaymentGateway}.
 *
 * <h2>Patrón Adapter — estructura</h2>
 * <pre>
 *  ┌─────────────────┐        usa         ┌──────────────────┐
 *  │  PaymentService │ ──────────────────▶ │  PaymentGateway  │  ← Target / Puerto
 *  │   (Cliente)     │                     │   (interfaz)     │
 *  └─────────────────┘                     └────────┬─────────┘
 *                                                   │ implementa
 *                                          ┌────────▼──────────────┐
 *                                          │  MercadoPagoAdapter   │  ← Adapter
 *                                          │  (este archivo)       │
 *                                          └────────┬──────────────┘
 *                                                   │ llama
 *                                          ┌────────▼──────────────┐
 *                                          │  API REST MercadoPago │  ← Adaptee
 *                                          │  /checkout/preferences│
 *                                          │  /v1/payments/{id}    │
 *                                          └───────────────────────┘
 * </pre>
 *
 * <h2>Extensibilidad</h2>
 * <p>Para agregar un nuevo proveedor de pago (p.ej. PayPal, Stripe), basta con:
 * <ol>
 *   <li>Crear una nueva clase que implemente {@link PaymentGateway} en este mismo paquete.</li>
 *   <li>Anotarla con {@code @Primary} o usar {@code @Qualifier} para seleccionarla.</li>
 *   <li>{@link ar.edu.utn.frc.tup.piii.services.PaymentService} no requiere ningún cambio.</li>
 * </ol>
 */
@Component
public class MercadoPagoAdapter implements PaymentGateway {

    private static final Logger logger = LoggerFactory.getLogger(MercadoPagoAdapter.class);

    /** Endpoint de la API de MP para crear preferencias de Checkout Pro. */
    private static final String PREFERENCES_URL = "https://api.mercadopago.com/checkout/preferences";

    /** Endpoint de la API de MP para consultar el estado de un pago por ID. */
    private static final String PAYMENTS_URL = "https://api.mercadopago.com/v1/payments/";

    /** Código de moneda ISO 4217 utilizado en todos los ítems de la preferencia. */
    private static final String CURRENCY_ARS = "ARS";

    /**
     * Valor del campo {@code auto_return}: MP redirige automáticamente al comprador
     * a {@code back_urls.success} tras un pago aprobado.
     * Solo se envía cuando la URL de éxito es pública (no localhost).
     */
    private static final String AUTO_RETURN_APPROVED = "approved";

    /** Estado de pago aprobado según la API de MercadoPago. */
    private static final String STATUS_APPROVED = "approved";
        
    /** Estado de pago rechazado según la API de MercadoPago. */
    private static final String STATUS_REJECTED = "rejected";

    private final RestTemplate restTemplate;
    private final PaymentConfig paymentConfig;

    /**
     * @param restTemplate  cliente HTTP para llamadas a la API de MP
     * @param paymentConfig configuración con token y URLs del entorno
     */
    public MercadoPagoAdapter(RestTemplate restTemplate, PaymentConfig paymentConfig) {
        this.restTemplate = restTemplate;
        this.paymentConfig = paymentConfig;
    }

    /**
     * Creates a Checkout Pro preference in MercadoPago and returns the payment URL.
     *
     * <p><b>Translation (Adapter):</b> converts the internal types
     * ({@link CreatePaymentRequest}, {@link PaymentItemDto}) to the MP format
     * ({@link MercadoPagoPreferenceRequest}, {@link MercadoPagoItem}) and returns
     * the result as a provider-agnostic {@link PaymentPreferenceResult}.
     *
     * <p><b>Local environments:</b> {@code notification_url} and {@code auto_return} are
     * omitted when the configured URLs point to {@code localhost}, because the MP
     * production API rejects non-public URLs for those fields.
     *
     * @param request        shopping cart and buyer context (provider-agnostic)
     * @param purchaseId     internal UUID sent as {@code external_reference} to MP to
     *                       correlate the webhook with the purchase in the database
     * @param idempotencyKey key sent as {@code X-Idempotency-Key} to MP so that a
     *                       retry of the preference creation does not duplicate it; if
     *                       null, the {@code purchaseId} is used as a stable fallback
     * @return result with the MP preference ID and the checkout URL
     * @throws RuntimeException if MP responds with a 4xx or 5xx error
     */
    @Override
    public PaymentPreferenceResult createPreference(CreatePaymentRequest request, String purchaseId,
            String idempotencyKey) {
        List<MercadoPagoItem> mpItems = request.getItems().stream()
                .map(this::toMercadoPagoItem)
                .toList();

        MercadoPagoPreferenceRequest mpRequest = new MercadoPagoPreferenceRequest();
        mpRequest.setItems(mpItems);
        mpRequest.setBackUrls(new MercadoPagoBackUrls(
                paymentConfig.getSuccessUrl(),
                paymentConfig.getFailureUrl(),
                paymentConfig.getPendingUrl()));
        mpRequest.setExternalReference(purchaseId);

        // auto_return solo se incluye si la URL de éxito es pública (no localhost).
        // MP exige que back_urls.success sea accesible para habilitar la redirección automática.
        String successUrl = paymentConfig.getSuccessUrl();
        if (successUrl != null && !successUrl.contains("localhost") && !successUrl.contains("127.0.0.1")) {
            mpRequest.setAutoReturn(AUTO_RETURN_APPROVED);
        }

        // notification_url solo se incluye si es una URL pública.
        // MP rechaza localhost en producción porque no puede hacer el POST del webhook.
        String notifUrl = paymentConfig.getNotificationUrl();
        if (notifUrl != null && !notifUrl.contains("localhost") && !notifUrl.contains("127.0.0.1")) {
            mpRequest.setNotificationUrl(notifUrl);
        }

        // X-Idempotency-Key: the client key, or the purchaseId as a stable fallback.
        String idemKey = (idempotencyKey != null && !idempotencyKey.isBlank())
                ? idempotencyKey
                : purchaseId;
        HttpEntity<MercadoPagoPreferenceRequest> entity = new HttpEntity<>(mpRequest, buildHeaders(idemKey));
        try {
            ResponseEntity<MercadoPagoPreferenceResponse> response = restTemplate.exchange(
                    PREFERENCES_URL, HttpMethod.POST, entity, MercadoPagoPreferenceResponse.class);

            MercadoPagoPreferenceResponse body = response.getBody();
            if (body == null) {
                throw new RuntimeException("Empty response from MercadoPago preferences API");
            }

            String checkoutUrl = paymentConfig.isSandbox()
                    ? body.getSandboxInitPoint()
                    : body.getInitPoint();

            return new PaymentPreferenceResult(purchaseId, body.getId(), checkoutUrl);

        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            logger.error("MercadoPago preferences API error — status: {}, body: {}",
                    ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new RuntimeException("MercadoPago error " + ex.getStatusCode()
                    + ": " + ex.getResponseBodyAsString(), ex);
        }
    }

    /**
     * Consulta el estado de un pago en MP y lo normaliza a {@code approved}/{@code rejected}.
     *
     * <p><b>Traducción (Adapter):</b> convierte el estado crudo de MP
     * (cadena como {@code "approved"}, {@code "rejected"}, {@code "in_process"}, etc.)
     * a los flags booleanos que usa internamente el sistema, sin exponer los valores
     * específicos de MP a la capa de servicio.
     *
     * <p>{@code externalReference} en la respuesta de MP contiene el {@code purchaseId}
     * que se envió al crear la preferencia, permitiendo correlacionar el pago con la
     * compra en la base de datos.
     *
     * @param mpPaymentId ID del pago en MP, recibido en el query param {@code ?id=} del webhook
     * @return resultado normalizado con {@code approved=true} o {@code rejected=true}
     * @throws RuntimeException si MP responde con un error 4xx o 5xx
     */
    @Override
    public PaymentNotificationResult processPayment(String mpPaymentId) {
        HttpEntity<Void> entity = new HttpEntity<>(buildHeaders(null));
        try {
            ResponseEntity<MercadoPagoPaymentResponse> response = restTemplate.exchange(
                    PAYMENTS_URL + mpPaymentId, HttpMethod.GET, entity, MercadoPagoPaymentResponse.class);

            MercadoPagoPaymentResponse body = response.getBody();
            if (body == null) {
                throw new RuntimeException("Empty response from MercadoPago payments API");
            }

            String status = body.getStatus();

            // Traducción del estado MP → flags booleanos internos (core del patrón Adapter)
            return new PaymentNotificationResult(
                    body.getExternalReference(),
                    STATUS_APPROVED.equals(status),
                    STATUS_REJECTED.equals(status),
                    status);

        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            logger.error("MercadoPago payments API error — status: {}, body: {}",
                    ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new RuntimeException("MercadoPago error " + ex.getStatusCode()
                    + ": " + ex.getResponseBodyAsString(), ex);
        }
    }

    /**
     * Convierte un ítem agnóstico al proveedor al formato específico de MercadoPago.
     *
     * <p>Este método encapsula la lógica de traducción del Adapter: el llamador
     * solo maneja {@link PaymentItemDto}, completamente desacoplado del esquema de MP.
     *
     * @param dto ítem en formato interno del sistema
     * @return ítem en el formato que espera la API de MercadoPago
     */
    private MercadoPagoItem toMercadoPagoItem(PaymentItemDto dto) {
        return new MercadoPagoItem(
                dto.getId(), dto.getTitle(), dto.getQuantity(), dto.getUnitPrice(), CURRENCY_ARS);
    }

    /**
     * Builds the HTTP headers required by the MercadoPago API.
     *
     * <p>Includes {@code Content-Type: application/json} and the access token as
     * {@code Authorization: Bearer <token>}. If an idempotency key is passed, it adds
     * {@code X-Idempotency-Key} so MP does not duplicate the operation on a retry.
     *
     * @param idempotencyKey key for the {@code X-Idempotency-Key} header; null omits it
     *                       (e.g. on status GET queries, which are already idempotent)
     * @return headers ready to attach to a request to MP
     */
    private HttpHeaders buildHeaders(String idempotencyKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(paymentConfig.getAccessToken());
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            headers.set("X-Idempotency-Key", idempotencyKey);
        }
        return headers;
    }
}