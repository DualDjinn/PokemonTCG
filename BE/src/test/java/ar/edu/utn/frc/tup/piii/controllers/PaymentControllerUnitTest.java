package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.configs.PaymentConfig;
import ar.edu.utn.frc.tup.piii.dtos.payment.CreatePaymentRequest;
import ar.edu.utn.frc.tup.piii.dtos.payment.PaymentPreferenceResult;
import ar.edu.utn.frc.tup.piii.dtos.payment.PurchaseStatusResponse;
import ar.edu.utn.frc.tup.piii.models.Player;
import ar.edu.utn.frc.tup.piii.services.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PaymentController}. The controller has no business
 * logic beyond a one-line delegation in {@code createPayment} and a 4-way
 * branch in the IPN webhook plus HMAC signature validation for webhook v2.
 *
 * <p>The webhook contract with MercadoPago: MP retries on non-200 responses,
 * so the endpoint must return 200 even when a notification is ignored
 * (unknown topic or missing payment id). Invalid signatures must return 403.
 */
class PaymentControllerUnitTest {

    private PaymentService paymentService;
    private PaymentConfig paymentConfig;
    private ObjectMapper objectMapper;
    private PaymentController controller;

    @BeforeEach
    void setUp() {
        paymentService = mock(PaymentService.class);
        paymentConfig = mock(PaymentConfig.class);
        objectMapper = new ObjectMapper();
        controller = new PaymentController(paymentService, paymentConfig, objectMapper);
    }

    /** Shortcut for the IPN legacy webhook shape (no headers, no body). */
    private ResponseEntity<Void> ipnWebhook(String paymentId, String topic) {
        return controller.handleWebhook(paymentId, topic, null, null, null);
    }

    // ============================================================
    // createPayment
    // ============================================================

    @Test
    void createPayment_DelegatesToServiceAndReturns200WithResult() {
        CreatePaymentRequest request = new CreatePaymentRequest();
        Player player = new Player();
        player.setId("player-1");
        PaymentPreferenceResult expected = new PaymentPreferenceResult();
        when(paymentService.createCheckout(request, null)).thenReturn(expected);

        ResponseEntity<PaymentPreferenceResult> response = controller.createPayment(null, player, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(expected);
    }

    @Test
    void createPayment_ForwardsRequestVerbatim() {
        CreatePaymentRequest request = new CreatePaymentRequest();
        Player player = new Player();
        player.setId("player-1");
        when(paymentService.createCheckout(request, null)).thenReturn(new PaymentPreferenceResult());

        controller.createPayment(null, player, request);

        verify(paymentService).createCheckout(request, null);
    }

    @Test
    void createPayment_ServiceThrowsRuntimeException_Propagates() {
        CreatePaymentRequest request = new CreatePaymentRequest();
        Player player = new Player();
        player.setId("player-1");
        when(paymentService.createCheckout(request, null))
                .thenThrow(new RuntimeException("MercadoPago API timeout"));

        assertThatThrownBy(() -> controller.createPayment(null, player, request))
                .as("controller must not hide service failures from the global handler")
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("MercadoPago API timeout");
    }

    // ============================================================
    // getPurchaseStatus
    // ============================================================

    @Test
    void getPurchaseStatus_Found_Returns200WithBody() {
        PurchaseStatusResponse status = new PurchaseStatusResponse();
        when(paymentService.getPurchaseStatus("purchase-1")).thenReturn(Optional.of(status));

        ResponseEntity<PurchaseStatusResponse> response = controller.getPurchaseStatus("purchase-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(status);
    }

    @Test
    void getPurchaseStatus_NotFound_Returns404() {
        when(paymentService.getPurchaseStatus("missing")).thenReturn(Optional.empty());

        ResponseEntity<PurchaseStatusResponse> response = controller.getPurchaseStatus("missing");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
    }

    // ============================================================
    // handleWebhook — IPN legacy path (topic + id in query, no body)
    // ============================================================

    @Test
    void webhook_IpnPaymentTopicWithId_ProcessesNotification() {
        ResponseEntity<Void> response = ipnWebhook("mp-payment-42", "payment");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(paymentService).handleWebhook("mp-payment-42");
    }

    @Test
    void webhook_IpnPaymentTopicNullId_IgnoresEvent() {
        ResponseEntity<Void> response = ipnWebhook(null, "payment");

        assertThat(response.getStatusCode())
                .as("MP retries on non-200, so even ignored events must reply 200")
                .isEqualTo(HttpStatus.OK);
        verifyNoInteractions(paymentService);
    }

    @Test
    void webhook_IpnNonPaymentTopicWithId_IgnoresEvent() {
        ResponseEntity<Void> response = ipnWebhook("mp-order-99", "merchant_order");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verifyNoInteractions(paymentService);
    }

    @Test
    void webhook_IpnNullTopicNullId_IgnoresEvent() {
        ResponseEntity<Void> response = ipnWebhook(null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verifyNoInteractions(paymentService);
    }

    @Test
    void webhook_IpnNullTopicWithId_IgnoresEvent() {
        ResponseEntity<Void> response = ipnWebhook("mp-payment-42", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verifyNoInteractions(paymentService);
    }

    @Test
    void webhook_IpnCaseSensitiveTopic_DoesNotMatch() {
        // "Payment" with capital P must not trigger processing.
        ResponseEntity<Void> response = ipnWebhook("mp-payment-42", "Payment");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verifyNoInteractions(paymentService);
    }

    @Test
    void webhook_IpnEmptyTopic_IgnoresEvent() {
        ResponseEntity<Void> response = ipnWebhook("mp-payment-42", "");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verifyNoInteractions(paymentService);
    }

    @Test
    void webhook_IpnEmptyPaymentId_PassesEmptyStringToService() {
        // Pin current behavior: paymentId == "" passes the != null guard and is
        // forwarded to the service. Tighten with !isBlank() if undesired.
        ResponseEntity<Void> response = ipnWebhook("", "payment");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(paymentService).handleWebhook("");
    }

    // ============================================================
    // handleWebhook — Webhook v2 path (body with data.id, headers)
    // ============================================================

    @Test
    void webhook_V2_ExtractsPaymentIdFromBodyDataId() {
        // In webhook v2 the query "id" is the NOTIFICATION id; the real payment
        // id lives in data.id of the JSON body. Verify the controller honors that.
        when(paymentConfig.getWebhookSecret()).thenReturn(null); // skip signature check
        String body = "{\"data\":{\"id\":\"real-payment-from-body\"}}";

        ResponseEntity<Void> response = controller.handleWebhook(
                "notif-id-not-payment", "payment",
                "ts=1,v1=deadbeef", null, body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(paymentService).handleWebhook("real-payment-from-body");
    }

    @Test
    void webhook_V2_MalformedBody_FallsBackToQueryParam() {
        // If the body cannot be parsed, the controller logs and uses the query
        // param "id" as the payment id (best-effort recovery).
        when(paymentConfig.getWebhookSecret()).thenReturn(null);

        ResponseEntity<Void> response = controller.handleWebhook(
                "fallback-id", "payment",
                "ts=1,v1=deadbeef", null, "not-json-{garbage");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(paymentService).handleWebhook("fallback-id");
    }

    @Test
    void webhook_V2_BodyMissingDataId_FallsBackToQueryParam() {
        when(paymentConfig.getWebhookSecret()).thenReturn(null);
        String body = "{\"other\":\"field\"}";

        ResponseEntity<Void> response = controller.handleWebhook(
                "fallback-id", "payment",
                "ts=1,v1=deadbeef", null, body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(paymentService).handleWebhook("fallback-id");
    }

    @Test
    void webhook_V2_BodyDataIdIsNull_FallsBackToQueryParam() {
        when(paymentConfig.getWebhookSecret()).thenReturn(null);
        String body = "{\"data\":{\"id\":null}}";

        ResponseEntity<Void> response = controller.handleWebhook(
                "fallback-id", "payment",
                "ts=1,v1=deadbeef", null, body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(paymentService).handleWebhook("fallback-id");
    }

    @Test
    void webhook_V2_BlankBody_FallsBackToQueryParam() {
        // body.isBlank() short-circuits the JSON parse; falls back to query id.
        when(paymentConfig.getWebhookSecret()).thenReturn(null);

        ResponseEntity<Void> response = controller.handleWebhook(
                "fallback-id", "payment", "ts=1,v1=deadbeef", null, "   ");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(paymentService).handleWebhook("fallback-id");
    }

    // ============================================================
    // handleWebhook — signature validation
    // ============================================================

    @Test
    void webhook_SecretNotConfigured_SkipsSignatureCheck() {
        when(paymentConfig.getWebhookSecret()).thenReturn(null);

        ResponseEntity<Void> response = controller.handleWebhook(
                "mp-payment-42", "payment",
                "ts=1,v1=invalid-but-not-checked", "req-id", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(paymentService).handleWebhook("mp-payment-42");
    }

    @Test
    void webhook_SecretBlank_SkipsSignatureCheck() {
        when(paymentConfig.getWebhookSecret()).thenReturn("   ");

        ResponseEntity<Void> response = controller.handleWebhook(
                "mp-payment-42", "payment",
                "ts=1,v1=invalid-but-not-checked", "req-id", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(paymentService).handleWebhook("mp-payment-42");
    }

    @Test
    void webhook_NoSignatureHeader_SkipsSignatureCheck() {
        // IPN legacy never sends x-signature. With secret configured but
        // header absent, the check must be skipped (otherwise IPN breaks).
        when(paymentConfig.getWebhookSecret()).thenReturn("00112233aabbccdd");

        ResponseEntity<Void> response = controller.handleWebhook(
                "mp-payment-42", "payment", null, null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(paymentService).handleWebhook("mp-payment-42");
    }

    @Test
    void webhook_InvalidSignature_Returns403AndSkipsService() {
        when(paymentConfig.getWebhookSecret()).thenReturn("00112233aabbccdd");

        ResponseEntity<Void> response = controller.handleWebhook(
                "mp-payment-42", "payment",
                "ts=1,v1=deadbeefdeadbeefdeadbeefdeadbeef", "req-id", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verifyNoInteractions(paymentService);
    }

    @Test
    void webhook_SignatureMissingTs_Returns403() {
        when(paymentConfig.getWebhookSecret()).thenReturn("00112233aabbccdd");

        ResponseEntity<Void> response = controller.handleWebhook(
                "mp-payment-42", "payment",
                "v1=deadbeef", "req-id", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verifyNoInteractions(paymentService);
    }

    @Test
    void webhook_SignatureMissingV1_Returns403() {
        when(paymentConfig.getWebhookSecret()).thenReturn("00112233aabbccdd");

        ResponseEntity<Void> response = controller.handleWebhook(
                "mp-payment-42", "payment",
                "ts=1", "req-id", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verifyNoInteractions(paymentService);
    }

    @Test
    void webhook_ValidSignature_Returns200AndProcesses() throws Exception {
        // Compute a valid HMAC-SHA256 the controller will accept. The manifest
        // template is documented in PaymentController#isValidSignature.
        String secretHex = "00112233aabbccdd";
        String paymentId = "mp-payment-42";
        String xRequestId = "req-abc";
        String ts = "1700000000";
        String manifest = "id:" + paymentId + ";request-id:" + xRequestId + ";ts:" + ts + ";";
        String v1 = hmacSha256Hex(secretHex, manifest);

        when(paymentConfig.getWebhookSecret()).thenReturn(secretHex);

        ResponseEntity<Void> response = controller.handleWebhook(
                paymentId, "payment",
                "ts=" + ts + ",v1=" + v1,
                xRequestId, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(paymentService).handleWebhook(paymentId);
    }

    @Test
    void webhook_ValidSignatureWithoutXRequestId_Returns200AndProcesses() throws Exception {
        // x-request-id is optional; the controller falls back to empty string
        // when computing the manifest. Valid signatures without it must work.
        String secretHex = "00112233aabbccdd";
        String paymentId = "mp-payment-42";
        String ts = "1700000000";
        String manifest = "id:" + paymentId + ";request-id:;ts:" + ts + ";";
        String v1 = hmacSha256Hex(secretHex, manifest);

        when(paymentConfig.getWebhookSecret()).thenReturn(secretHex);

        ResponseEntity<Void> response = controller.handleWebhook(
                paymentId, "payment",
                "ts=" + ts + ",v1=" + v1, null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(paymentService).handleWebhook(paymentId);
    }

    private static String hmacSha256Hex(String secretHex, String manifest) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(hexToBytes(secretHex), "HmacSHA256"));
        byte[] hash = mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
