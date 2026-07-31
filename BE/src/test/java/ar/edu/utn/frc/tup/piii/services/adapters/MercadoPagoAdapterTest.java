package ar.edu.utn.frc.tup.piii.services.adapters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import ar.edu.utn.frc.tup.piii.configs.PaymentConfig;
import ar.edu.utn.frc.tup.piii.dtos.payment.CreatePaymentRequest;
import ar.edu.utn.frc.tup.piii.dtos.payment.PaymentItemDto;
import ar.edu.utn.frc.tup.piii.dtos.payment.PaymentNotificationResult;
import ar.edu.utn.frc.tup.piii.dtos.payment.PaymentPreferenceResult;
import ar.edu.utn.frc.tup.piii.dtos.payment.mp.MercadoPagoPaymentResponse;
import ar.edu.utn.frc.tup.piii.dtos.payment.mp.MercadoPagoPreferenceRequest;
import ar.edu.utn.frc.tup.piii.dtos.payment.mp.MercadoPagoPreferenceResponse;

class MercadoPagoAdapterTest {

    private RestTemplate restTemplate;
    private PaymentConfig paymentConfig;
    private MercadoPagoAdapter adapter;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        paymentConfig = mock(PaymentConfig.class);
        adapter = new MercadoPagoAdapter(restTemplate, paymentConfig);

        when(paymentConfig.getAccessToken()).thenReturn("TEST-token");
        when(paymentConfig.getSuccessUrl()).thenReturn("http://localhost:4200/payment/success");
        when(paymentConfig.getFailureUrl()).thenReturn("http://localhost:4200/payment/failure");
        when(paymentConfig.getPendingUrl()).thenReturn("http://localhost:4200/payment/pending");
        when(paymentConfig.getNotificationUrl()).thenReturn("http://localhost:8080/api/payments/webhook");
        when(paymentConfig.isSandbox()).thenReturn(true);
    }

    private CreatePaymentRequest buildRequest() {
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setPlayerId("p1");
        request.setCoinsToAdd(100);
        request.setItems(List.of(
                new PaymentItemDto("pkg-100", "100 Coins", 1, BigDecimal.ONE)));
        return request;
    }

    // ───────── createPreference — sandbox ─────────

    @Test
    void createPreference_Sandbox_ReturnsSandboxUrl() {
        MercadoPagoPreferenceResponse mpResponse = new MercadoPagoPreferenceResponse();
        mpResponse.setId("pref-123");
        mpResponse.setInitPoint("https://mp.com/pay");
        mpResponse.setSandboxInitPoint("https://sandbox.mp.com/pay");

        when(restTemplate.exchange(
                eq("https://api.mercadopago.com/checkout/preferences"),
                eq(HttpMethod.POST), any(HttpEntity.class),
                eq(MercadoPagoPreferenceResponse.class)))
                .thenReturn(new ResponseEntity<>(mpResponse, HttpStatus.CREATED));

        PaymentPreferenceResult result = adapter.createPreference(buildRequest(), "purchase-1", null);

        assertThat(result.getPurchaseId()).isEqualTo("purchase-1");
        assertThat(result.getPreferenceId()).isEqualTo("pref-123");
        assertThat(result.getCheckoutUrl()).isEqualTo("https://sandbox.mp.com/pay");
    }

    // ───────── createPreference — production ─────────

    @Test
    void createPreference_Production_ReturnsInitPoint() {
        when(paymentConfig.isSandbox()).thenReturn(false);

        MercadoPagoPreferenceResponse mpResponse = new MercadoPagoPreferenceResponse();
        mpResponse.setId("pref-456");
        mpResponse.setInitPoint("https://mp.com/pay");
        mpResponse.setSandboxInitPoint("https://sandbox.mp.com/pay");

        when(restTemplate.exchange(
                eq("https://api.mercadopago.com/checkout/preferences"),
                eq(HttpMethod.POST), any(HttpEntity.class),
                eq(MercadoPagoPreferenceResponse.class)))
                .thenReturn(new ResponseEntity<>(mpResponse, HttpStatus.CREATED));

        PaymentPreferenceResult result = adapter.createPreference(buildRequest(), "purchase-2", null);

        assertThat(result.getCheckoutUrl()).isEqualTo("https://mp.com/pay");
    }

    // ───────── createPreference — localhost omits auto_return and notification_url ─────────

    @SuppressWarnings("unchecked")
    @Test
    void createPreference_Localhost_OmitsAutoReturnAndNotification() {
        MercadoPagoPreferenceResponse mpResponse = new MercadoPagoPreferenceResponse();
        mpResponse.setId("pref-789");
        mpResponse.setSandboxInitPoint("https://sandbox.mp.com/pay");

        ArgumentCaptor<HttpEntity<MercadoPagoPreferenceRequest>> captor =
                ArgumentCaptor.forClass(HttpEntity.class);

        when(restTemplate.exchange(
                eq("https://api.mercadopago.com/checkout/preferences"),
                eq(HttpMethod.POST), captor.capture(),
                eq(MercadoPagoPreferenceResponse.class)))
                .thenReturn(new ResponseEntity<>(mpResponse, HttpStatus.CREATED));

        adapter.createPreference(buildRequest(), "purchase-3", null);

        MercadoPagoPreferenceRequest body = captor.getValue().getBody();
        assertThat(body).isNotNull();
        assertThat(body.getAutoReturn()).isNull();
        assertThat(body.getNotificationUrl()).isNull();
    }

    // ───────── createPreference — public URLs include auto_return ─────────

    @SuppressWarnings("unchecked")
    @Test
    void createPreference_PublicUrls_IncludesAutoReturnAndNotification() {
        when(paymentConfig.getSuccessUrl()).thenReturn("https://myapp.com/success");
        when(paymentConfig.getNotificationUrl()).thenReturn("https://myapp.com/webhook");

        MercadoPagoPreferenceResponse mpResponse = new MercadoPagoPreferenceResponse();
        mpResponse.setId("pref-pub");
        mpResponse.setSandboxInitPoint("https://sandbox.mp.com/pay");

        ArgumentCaptor<HttpEntity<MercadoPagoPreferenceRequest>> captor =
                ArgumentCaptor.forClass(HttpEntity.class);

        when(restTemplate.exchange(
                eq("https://api.mercadopago.com/checkout/preferences"),
                eq(HttpMethod.POST), captor.capture(),
                eq(MercadoPagoPreferenceResponse.class)))
                .thenReturn(new ResponseEntity<>(mpResponse, HttpStatus.CREATED));

        adapter.createPreference(buildRequest(), "purchase-pub", null);

        MercadoPagoPreferenceRequest body = captor.getValue().getBody();
        assertThat(body).isNotNull();
        assertThat(body.getAutoReturn()).isEqualTo("approved");
        assertThat(body.getNotificationUrl()).isEqualTo("https://myapp.com/webhook");
    }

    // ───────── createPreference — 127.0.0.1 also omits ─────────

    @SuppressWarnings("unchecked")
    @Test
    void createPreference_LoopbackAddress_OmitsAutoReturn() {
        when(paymentConfig.getSuccessUrl()).thenReturn("http://127.0.0.1:4200/success");
        when(paymentConfig.getNotificationUrl()).thenReturn("http://127.0.0.1:8080/webhook");

        MercadoPagoPreferenceResponse mpResponse = new MercadoPagoPreferenceResponse();
        mpResponse.setId("pref-loop");
        mpResponse.setSandboxInitPoint("https://sandbox.mp.com/pay");

        ArgumentCaptor<HttpEntity<MercadoPagoPreferenceRequest>> captor =
                ArgumentCaptor.forClass(HttpEntity.class);

        when(restTemplate.exchange(
                eq("https://api.mercadopago.com/checkout/preferences"),
                eq(HttpMethod.POST), captor.capture(),
                eq(MercadoPagoPreferenceResponse.class)))
                .thenReturn(new ResponseEntity<>(mpResponse, HttpStatus.CREATED));

        adapter.createPreference(buildRequest(), "purchase-loop", null);

        MercadoPagoPreferenceRequest body = captor.getValue().getBody();
        assertThat(body.getAutoReturn()).isNull();
        assertThat(body.getNotificationUrl()).isNull();
    }

    // ───────── createPreference — empty response body ─────────

    @Test
    void createPreference_EmptyResponseBody_Throws() {
        when(restTemplate.exchange(
                eq("https://api.mercadopago.com/checkout/preferences"),
                eq(HttpMethod.POST), any(HttpEntity.class),
                eq(MercadoPagoPreferenceResponse.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.CREATED).body(null));

        assertThatThrownBy(() -> adapter.createPreference(buildRequest(), "purchase-null", null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Empty response");
    }

    // ───────── createPreference — HTTP errors ─────────

    @Test
    void createPreference_ClientError_Throws() {
        when(restTemplate.exchange(
                eq("https://api.mercadopago.com/checkout/preferences"),
                eq(HttpMethod.POST), any(HttpEntity.class),
                eq(MercadoPagoPreferenceResponse.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Bad Request"));

        assertThatThrownBy(() -> adapter.createPreference(buildRequest(), "purchase-err", null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("MercadoPago error");
    }

    @Test
    void createPreference_ServerError_Throws() {
        when(restTemplate.exchange(
                eq("https://api.mercadopago.com/checkout/preferences"),
                eq(HttpMethod.POST), any(HttpEntity.class),
                eq(MercadoPagoPreferenceResponse.class)))
                .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> adapter.createPreference(buildRequest(), "purchase-500", null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("MercadoPago error");
    }

    // ───────── processPayment — approved ─────────

    @Test
    void processPayment_Approved_ReturnsTrueApproved() {
        MercadoPagoPaymentResponse mpResponse = new MercadoPagoPaymentResponse();
        mpResponse.setId(12345L);
        mpResponse.setStatus("approved");
        mpResponse.setExternalReference("purchase-1");

        when(restTemplate.exchange(
                eq("https://api.mercadopago.com/v1/payments/mp-pay-1"),
                eq(HttpMethod.GET), any(HttpEntity.class),
                eq(MercadoPagoPaymentResponse.class)))
                .thenReturn(new ResponseEntity<>(mpResponse, HttpStatus.OK));

        PaymentNotificationResult result = adapter.processPayment("mp-pay-1");

        assertThat(result.isApproved()).isTrue();
        assertThat(result.isRejected()).isFalse();
        assertThat(result.getPurchaseId()).isEqualTo("purchase-1");
        assertThat(result.getMpStatus()).isEqualTo("approved");
    }

    // ───────── processPayment — rejected ─────────

    @Test
    void processPayment_Rejected_ReturnsTrueRejected() {
        MercadoPagoPaymentResponse mpResponse = new MercadoPagoPaymentResponse();
        mpResponse.setId(12346L);
        mpResponse.setStatus("rejected");
        mpResponse.setExternalReference("purchase-2");

        when(restTemplate.exchange(
                eq("https://api.mercadopago.com/v1/payments/mp-pay-2"),
                eq(HttpMethod.GET), any(HttpEntity.class),
                eq(MercadoPagoPaymentResponse.class)))
                .thenReturn(new ResponseEntity<>(mpResponse, HttpStatus.OK));

        PaymentNotificationResult result = adapter.processPayment("mp-pay-2");

        assertThat(result.isApproved()).isFalse();
        assertThat(result.isRejected()).isTrue();
    }

    // ───────── processPayment — pending (neither approved nor rejected) ─────────

    @Test
    void processPayment_Pending_BothFalse() {
        MercadoPagoPaymentResponse mpResponse = new MercadoPagoPaymentResponse();
        mpResponse.setId(12347L);
        mpResponse.setStatus("in_process");
        mpResponse.setExternalReference("purchase-3");

        when(restTemplate.exchange(
                eq("https://api.mercadopago.com/v1/payments/mp-pay-3"),
                eq(HttpMethod.GET), any(HttpEntity.class),
                eq(MercadoPagoPaymentResponse.class)))
                .thenReturn(new ResponseEntity<>(mpResponse, HttpStatus.OK));

        PaymentNotificationResult result = adapter.processPayment("mp-pay-3");

        assertThat(result.isApproved()).isFalse();
        assertThat(result.isRejected()).isFalse();
        assertThat(result.getMpStatus()).isEqualTo("in_process");
    }

    // ───────── processPayment — empty response body ─────────

    @Test
    void processPayment_EmptyResponseBody_Throws() {
        when(restTemplate.exchange(
                eq("https://api.mercadopago.com/v1/payments/mp-pay-null"),
                eq(HttpMethod.GET), any(HttpEntity.class),
                eq(MercadoPagoPaymentResponse.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.OK).body(null));

        assertThatThrownBy(() -> adapter.processPayment("mp-pay-null"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Empty response");
    }

    // ───────── processPayment — HTTP errors ─────────

    @Test
    void processPayment_ClientError_Throws() {
        when(restTemplate.exchange(
                eq("https://api.mercadopago.com/v1/payments/mp-pay-err"),
                eq(HttpMethod.GET), any(HttpEntity.class),
                eq(MercadoPagoPaymentResponse.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND, "Not Found"));

        assertThatThrownBy(() -> adapter.processPayment("mp-pay-err"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("MercadoPago error");
    }

    @Test
    void processPayment_ServerError_Throws() {
        when(restTemplate.exchange(
                eq("https://api.mercadopago.com/v1/payments/mp-pay-500"),
                eq(HttpMethod.GET), any(HttpEntity.class),
                eq(MercadoPagoPaymentResponse.class)))
                .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> adapter.processPayment("mp-pay-500"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("MercadoPago error");
    }

    // ───────── createPreference — items mapping ─────────

    @SuppressWarnings("unchecked")
    @Test
    void createPreference_MapsItemsCorrectly() {
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setPlayerId("p1");
        request.setItems(List.of(
                new PaymentItemDto("pkg-100", "100 Coins", 2, new BigDecimal("1.50"))));

        MercadoPagoPreferenceResponse mpResponse = new MercadoPagoPreferenceResponse();
        mpResponse.setId("pref-items");
        mpResponse.setSandboxInitPoint("https://sandbox.mp.com/pay");

        ArgumentCaptor<HttpEntity<MercadoPagoPreferenceRequest>> captor =
                ArgumentCaptor.forClass(HttpEntity.class);

        when(restTemplate.exchange(
                eq("https://api.mercadopago.com/checkout/preferences"),
                eq(HttpMethod.POST), captor.capture(),
                eq(MercadoPagoPreferenceResponse.class)))
                .thenReturn(new ResponseEntity<>(mpResponse, HttpStatus.CREATED));

        adapter.createPreference(request, "purchase-items", null);

        MercadoPagoPreferenceRequest body = captor.getValue().getBody();
        assertThat(body.getItems()).hasSize(1);
        assertThat(body.getItems().get(0).getId()).isEqualTo("pkg-100");
        assertThat(body.getItems().get(0).getTitle()).isEqualTo("100 Coins");
        assertThat(body.getItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(body.getItems().get(0).getCurrencyId()).isEqualTo("ARS");
        assertThat(body.getExternalReference()).isEqualTo("purchase-items");
    }

    // ───────── createPreference — headers ─────────

    @SuppressWarnings("unchecked")
    @Test
    void createPreference_SetsAuthorizationHeader() {
        MercadoPagoPreferenceResponse mpResponse = new MercadoPagoPreferenceResponse();
        mpResponse.setId("pref-auth");
        mpResponse.setSandboxInitPoint("https://sandbox.mp.com/pay");

        ArgumentCaptor<HttpEntity<MercadoPagoPreferenceRequest>> captor =
                ArgumentCaptor.forClass(HttpEntity.class);

        when(restTemplate.exchange(
                eq("https://api.mercadopago.com/checkout/preferences"),
                eq(HttpMethod.POST), captor.capture(),
                eq(MercadoPagoPreferenceResponse.class)))
                .thenReturn(new ResponseEntity<>(mpResponse, HttpStatus.CREATED));

        adapter.createPreference(buildRequest(), "purchase-auth", "idem-key-1");

        assertThat(captor.getValue().getHeaders().getFirst("Authorization"))
                .isEqualTo("Bearer TEST-token");
        assertThat(captor.getValue().getHeaders().getContentType().toString())
                .contains("application/json");
        // The client's idempotency key travels as X-Idempotency-Key.
        assertThat(captor.getValue().getHeaders().getFirst("X-Idempotency-Key"))
                .isEqualTo("idem-key-1");
    }

    // ───────── createPreference — idempotency key fallback ─────────

    @SuppressWarnings("unchecked")
    @Test
    void createPreference_NullKey_UsesPurchaseIdAsIdempotencyKey() {
        MercadoPagoPreferenceResponse mpResponse = new MercadoPagoPreferenceResponse();
        mpResponse.setId("pref-fallback");
        mpResponse.setSandboxInitPoint("https://sandbox.mp.com/pay");

        ArgumentCaptor<HttpEntity<MercadoPagoPreferenceRequest>> captor =
                ArgumentCaptor.forClass(HttpEntity.class);

        when(restTemplate.exchange(
                eq("https://api.mercadopago.com/checkout/preferences"),
                eq(HttpMethod.POST), captor.capture(),
                eq(MercadoPagoPreferenceResponse.class)))
                .thenReturn(new ResponseEntity<>(mpResponse, HttpStatus.CREATED));

        // Without a client key, the adapter uses the purchaseId as a stable X-Idempotency-Key.
        adapter.createPreference(buildRequest(), "purchase-fallback", null);

        assertThat(captor.getValue().getHeaders().getFirst("X-Idempotency-Key"))
                .isEqualTo("purchase-fallback");
    }

    // ───────── createPreference — null URLs omit auto_return / notification ─────────

    @SuppressWarnings("unchecked")
    @Test
    void createPreference_NullUrls_OmitsAutoReturnAndNotification() {
        when(paymentConfig.getSuccessUrl()).thenReturn(null);
        when(paymentConfig.getNotificationUrl()).thenReturn(null);

        MercadoPagoPreferenceResponse mpResponse = new MercadoPagoPreferenceResponse();
        mpResponse.setId("pref-nullurls");
        mpResponse.setSandboxInitPoint("https://sandbox.mp.com/pay");

        ArgumentCaptor<HttpEntity<MercadoPagoPreferenceRequest>> captor =
                ArgumentCaptor.forClass(HttpEntity.class);

        when(restTemplate.exchange(
                eq("https://api.mercadopago.com/checkout/preferences"),
                eq(HttpMethod.POST), captor.capture(),
                eq(MercadoPagoPreferenceResponse.class)))
                .thenReturn(new ResponseEntity<>(mpResponse, HttpStatus.CREATED));

        adapter.createPreference(buildRequest(), "purchase-nullurls", null);

        MercadoPagoPreferenceRequest body = captor.getValue().getBody();
        assertThat(body.getAutoReturn()).isNull();
        assertThat(body.getNotificationUrl()).isNull();
    }

    // ───────── createPreference — blank idempotency key falls back to purchaseId ─────────

    @SuppressWarnings("unchecked")
    @Test
    void createPreference_BlankKey_UsesPurchaseIdAsIdempotencyKey() {
        MercadoPagoPreferenceResponse mpResponse = new MercadoPagoPreferenceResponse();
        mpResponse.setId("pref-blankkey");
        mpResponse.setSandboxInitPoint("https://sandbox.mp.com/pay");

        ArgumentCaptor<HttpEntity<MercadoPagoPreferenceRequest>> captor =
                ArgumentCaptor.forClass(HttpEntity.class);

        when(restTemplate.exchange(
                eq("https://api.mercadopago.com/checkout/preferences"),
                eq(HttpMethod.POST), captor.capture(),
                eq(MercadoPagoPreferenceResponse.class)))
                .thenReturn(new ResponseEntity<>(mpResponse, HttpStatus.CREATED));

        adapter.createPreference(buildRequest(), "purchase-blank", "   ");

        assertThat(captor.getValue().getHeaders().getFirst("X-Idempotency-Key"))
                .isEqualTo("purchase-blank");
    }
}
