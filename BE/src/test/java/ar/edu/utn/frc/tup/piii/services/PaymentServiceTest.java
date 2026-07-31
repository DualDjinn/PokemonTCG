package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.payment.CreatePaymentRequest;
import ar.edu.utn.frc.tup.piii.dtos.payment.PaymentItemDto;
import ar.edu.utn.frc.tup.piii.dtos.payment.PaymentNotificationResult;
import ar.edu.utn.frc.tup.piii.dtos.payment.PaymentPreferenceResult;
import ar.edu.utn.frc.tup.piii.dtos.payment.PurchaseStatusResponse;
import ar.edu.utn.frc.tup.piii.models.Player;
import ar.edu.utn.frc.tup.piii.models.Purchase;
import ar.edu.utn.frc.tup.piii.repositories.IdempotencyRecordRepository;
import ar.edu.utn.frc.tup.piii.repositories.PlayerRepository;
import ar.edu.utn.frc.tup.piii.repositories.PurchaseRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentGateway paymentGateway;
    @Mock
    private PurchaseRepository purchaseRepository;
    @Mock
    private PlayerRepository playerRepository;
    @Mock
    private ObjectMapper objectMapper;

    private PaymentService paymentService;

    private static final String MP_PAYMENT_ID = "mp-payment-1";
    private static final String PURCHASE_ID = "purchase-1";
    private static final String PLAYER_ID = "player-1";

    private Purchase purchase;
    private Player player;

    @BeforeEach
    void setUp() {
        // Real IdempotencyService with a mock repo: a null key does passthrough
        // (does not touch the repo), preserving the behavior of the existing tests.
        paymentService = new PaymentService(paymentGateway, purchaseRepository, playerRepository,
                objectMapper, new IdempotencyService(mock(IdempotencyRecordRepository.class), new ObjectMapper()));

        purchase = new Purchase();
        purchase.setId(PURCHASE_ID);
        purchase.setPlayerId(PLAYER_ID);
        purchase.setCoinsToAdd(1000);
        purchase.setApproved(false);
        purchase.setRejected(false);
        purchase.setCoinsCredited(false);

        player = new Player();
        player.setId(PLAYER_ID);
        player.setCoins(500);
    }

    private PaymentNotificationResult approved() {
        return new PaymentNotificationResult(PURCHASE_ID, true, false, "approved");
    }

    private PaymentNotificationResult rejected() {
        return new PaymentNotificationResult(PURCHASE_ID, false, true, "rejected");
    }

    // ───────── handleWebhook ─────────

    @Test
    void handleWebhook_approvedPurchase_creditsCoinsOnceAndMarksCredited() {
        when(paymentGateway.processPayment(MP_PAYMENT_ID)).thenReturn(approved());
        when(purchaseRepository.findByIdForUpdate(PURCHASE_ID)).thenReturn(Optional.of(purchase));
        when(playerRepository.findById(PLAYER_ID)).thenReturn(Optional.of(player));

        paymentService.handleWebhook(MP_PAYMENT_ID);

        assertThat(player.getCoins()).isEqualTo(1500);
        assertThat(purchase.isCoinsCredited()).isTrue();
        assertThat(purchase.isApproved()).isTrue();
        verify(playerRepository, times(1)).save(player);
    }

    @Test
    void handleWebhook_redeliveredTwice_creditsCoinsExactlyOnce() {
        when(paymentGateway.processPayment(MP_PAYMENT_ID)).thenReturn(approved());
        when(purchaseRepository.findByIdForUpdate(PURCHASE_ID)).thenReturn(Optional.of(purchase));
        when(playerRepository.findById(PLAYER_ID)).thenReturn(Optional.of(player));

        paymentService.handleWebhook(MP_PAYMENT_ID);
        paymentService.handleWebhook(MP_PAYMENT_ID);

        assertThat(player.getCoins()).isEqualTo(1500);
        assertThat(purchase.isCoinsCredited()).isTrue();
        verify(playerRepository, times(1)).save(player);
    }

    @Test
    void handleWebhook_alreadyCredited_doesNotCreditAgain() {
        purchase.setCoinsCredited(true);
        when(paymentGateway.processPayment(MP_PAYMENT_ID)).thenReturn(approved());
        when(purchaseRepository.findByIdForUpdate(PURCHASE_ID)).thenReturn(Optional.of(purchase));

        paymentService.handleWebhook(MP_PAYMENT_ID);

        assertThat(player.getCoins()).isEqualTo(500);
        verify(playerRepository, never()).findById(any());
        verify(playerRepository, never()).save(any());
    }

    @Test
    void handleWebhook_rejectedPurchase_doesNotCreditCoins() {
        when(paymentGateway.processPayment(MP_PAYMENT_ID)).thenReturn(rejected());
        when(purchaseRepository.findByIdForUpdate(PURCHASE_ID)).thenReturn(Optional.of(purchase));

        paymentService.handleWebhook(MP_PAYMENT_ID);

        assertThat(player.getCoins()).isEqualTo(500);
        assertThat(purchase.isCoinsCredited()).isFalse();
        assertThat(purchase.isRejected()).isTrue();
        verify(playerRepository, never()).save(any());
    }

    @Test
    void handleWebhook_purchaseNotFound_noErrorNoCredit() {
        when(paymentGateway.processPayment(MP_PAYMENT_ID)).thenReturn(approved());
        when(purchaseRepository.findByIdForUpdate(PURCHASE_ID)).thenReturn(Optional.empty());

        paymentService.handleWebhook(MP_PAYMENT_ID);

        verify(playerRepository, never()).save(any());
    }

    @Test
    void handleWebhook_nullPurchaseId_noAction() {
        PaymentNotificationResult result = new PaymentNotificationResult(null, true, false, "approved");
        when(paymentGateway.processPayment(MP_PAYMENT_ID)).thenReturn(result);

        paymentService.handleWebhook(MP_PAYMENT_ID);

        verify(purchaseRepository, never()).findByIdForUpdate(any());
        verify(playerRepository, never()).save(any());
    }

    @Test
    void handleWebhook_pendingStatus_neitherApprovedNorRejected() {
        PaymentNotificationResult pending = new PaymentNotificationResult(PURCHASE_ID, false, false, "in_process");
        when(paymentGateway.processPayment(MP_PAYMENT_ID)).thenReturn(pending);
        when(purchaseRepository.findByIdForUpdate(PURCHASE_ID)).thenReturn(Optional.of(purchase));

        paymentService.handleWebhook(MP_PAYMENT_ID);

        assertThat(purchase.isApproved()).isFalse();
        assertThat(purchase.isRejected()).isFalse();
        assertThat(purchase.isCoinsCredited()).isFalse();
        verify(playerRepository, never()).save(any());
    }

    @Test
    void handleWebhook_setsUpdatedAtAndMpFields() {
        when(paymentGateway.processPayment(MP_PAYMENT_ID)).thenReturn(approved());
        when(purchaseRepository.findByIdForUpdate(PURCHASE_ID)).thenReturn(Optional.of(purchase));
        when(playerRepository.findById(PLAYER_ID)).thenReturn(Optional.of(player));

        paymentService.handleWebhook(MP_PAYMENT_ID);

        assertThat(purchase.getMpPaymentId()).isEqualTo(MP_PAYMENT_ID);
        assertThat(purchase.getMpStatus()).isEqualTo("approved");
        assertThat(purchase.getUpdatedAt()).isNotNull();
    }

    @Test
    void handleWebhook_playerNotFound_creditsNothing() {
        when(paymentGateway.processPayment(MP_PAYMENT_ID)).thenReturn(approved());
        when(purchaseRepository.findByIdForUpdate(PURCHASE_ID)).thenReturn(Optional.of(purchase));
        when(playerRepository.findById(PLAYER_ID)).thenReturn(Optional.empty());

        paymentService.handleWebhook(MP_PAYMENT_ID);

        assertThat(purchase.isCoinsCredited()).isTrue();
        assertThat(player.getCoins()).isEqualTo(500);
    }

    // ───────── createCheckout ─────────

    @Test
    void createCheckout_PersistsPurchaseAndReturnsResult() throws Exception {
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setPlayerId(PLAYER_ID);
        request.setCoinsToAdd(100);
        request.setItems(List.of(
                new PaymentItemDto("pkg-100", "100 Coins", 1, BigDecimal.ONE)));

        PaymentPreferenceResult prefResult = new PaymentPreferenceResult("any-id", "pref-123", "https://checkout.url");
        when(paymentGateway.createPreference(eq(request), anyString(), any())).thenReturn(prefResult);
        when(objectMapper.writeValueAsString(any())).thenReturn("[{\"id\":\"pkg-100\"}]");

        PaymentPreferenceResult result = paymentService.createCheckout(request, null);

        assertThat(result.getPreferenceId()).isEqualTo("pref-123");
        assertThat(result.getCheckoutUrl()).isEqualTo("https://checkout.url");

        ArgumentCaptor<Purchase> captor = ArgumentCaptor.forClass(Purchase.class);
        verify(purchaseRepository, times(2)).save(captor.capture());
        Purchase saved = captor.getAllValues().get(0);
        assertThat(saved.getPlayerId()).isEqualTo(PLAYER_ID);
        assertThat(saved.getTotalAmount()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(saved.isApproved()).isFalse();
        assertThat(saved.isRejected()).isFalse();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void createCheckout_CalculatesCoins_1Peso_100Coins() throws Exception {
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setPlayerId(PLAYER_ID);
        request.setItems(List.of(
                new PaymentItemDto("pkg", "Coins", 1, BigDecimal.ONE)));

        when(paymentGateway.createPreference(any(), anyString(), any()))
                .thenReturn(new PaymentPreferenceResult("id", "pref", "url"));
        when(objectMapper.writeValueAsString(any())).thenReturn("[]");

        paymentService.createCheckout(request, null);

        ArgumentCaptor<Purchase> captor = ArgumentCaptor.forClass(Purchase.class);
        verify(purchaseRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getCoinsToAdd()).isEqualTo(100);
    }

    @Test
    void createCheckout_CalculatesCoins_2Pesos_500Coins() throws Exception {
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setPlayerId(PLAYER_ID);
        request.setItems(List.of(
                new PaymentItemDto("pkg", "Coins", 1, new BigDecimal("2"))));

        when(paymentGateway.createPreference(any(), anyString(), any()))
                .thenReturn(new PaymentPreferenceResult("id", "pref", "url"));
        when(objectMapper.writeValueAsString(any())).thenReturn("[]");

        paymentService.createCheckout(request, null);

        ArgumentCaptor<Purchase> captor = ArgumentCaptor.forClass(Purchase.class);
        verify(purchaseRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getCoinsToAdd()).isEqualTo(500);
    }

    @Test
    void createCheckout_CalculatesCoins_3Pesos_1000Coins() throws Exception {
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setPlayerId(PLAYER_ID);
        request.setItems(List.of(
                new PaymentItemDto("pkg", "Coins", 1, new BigDecimal("3"))));

        when(paymentGateway.createPreference(any(), anyString(), any()))
                .thenReturn(new PaymentPreferenceResult("id", "pref", "url"));
        when(objectMapper.writeValueAsString(any())).thenReturn("[]");

        paymentService.createCheckout(request, null);

        ArgumentCaptor<Purchase> captor = ArgumentCaptor.forClass(Purchase.class);
        verify(purchaseRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getCoinsToAdd()).isEqualTo(1000);
    }

    @Test
    void createCheckout_CalculatesCoins_4Pesos_5000Coins() throws Exception {
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setPlayerId(PLAYER_ID);
        request.setItems(List.of(
                new PaymentItemDto("pkg", "Coins", 1, new BigDecimal("4"))));

        when(paymentGateway.createPreference(any(), anyString(), any()))
                .thenReturn(new PaymentPreferenceResult("id", "pref", "url"));
        when(objectMapper.writeValueAsString(any())).thenReturn("[]");

        paymentService.createCheckout(request, null);

        ArgumentCaptor<Purchase> captor = ArgumentCaptor.forClass(Purchase.class);
        verify(purchaseRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getCoinsToAdd()).isEqualTo(5000);
    }

    @Test
    void createCheckout_UnknownAmount_Returns0Coins() throws Exception {
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setPlayerId(PLAYER_ID);
        request.setItems(List.of(
                new PaymentItemDto("pkg", "Coins", 1, new BigDecimal("99"))));

        when(paymentGateway.createPreference(any(), anyString(), any()))
                .thenReturn(new PaymentPreferenceResult("id", "pref", "url"));
        when(objectMapper.writeValueAsString(any())).thenReturn("[]");

        paymentService.createCheckout(request, null);

        ArgumentCaptor<Purchase> captor = ArgumentCaptor.forClass(Purchase.class);
        verify(purchaseRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getCoinsToAdd()).isEqualTo(0);
    }

    @Test
    void createCheckout_UpdatesPreferenceIdAfterGatewayCall() throws Exception {
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setPlayerId(PLAYER_ID);
        request.setItems(List.of(
                new PaymentItemDto("pkg", "Coins", 1, BigDecimal.ONE)));

        when(paymentGateway.createPreference(any(), anyString(), any()))
                .thenReturn(new PaymentPreferenceResult("id", "pref-abc", "url"));
        when(objectMapper.writeValueAsString(any())).thenReturn("[]");

        paymentService.createCheckout(request, null);

        ArgumentCaptor<Purchase> captor = ArgumentCaptor.forClass(Purchase.class);
        verify(purchaseRepository, times(2)).save(captor.capture());
        Purchase secondSave = captor.getAllValues().get(1);
        assertThat(secondSave.getPreferenceId()).isEqualTo("pref-abc");
    }

    @Test
    void createCheckout_MultipleItems_CalculatesTotalCorrectly() throws Exception {
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setPlayerId(PLAYER_ID);
        request.setItems(List.of(
                new PaymentItemDto("pkg1", "Coins A", 2, BigDecimal.ONE),
                new PaymentItemDto("pkg2", "Coins B", 1, new BigDecimal("2"))));

        when(paymentGateway.createPreference(any(), anyString(), any()))
                .thenReturn(new PaymentPreferenceResult("id", "pref", "url"));
        when(objectMapper.writeValueAsString(any())).thenReturn("[]");

        paymentService.createCheckout(request, null);

        ArgumentCaptor<Purchase> captor = ArgumentCaptor.forClass(Purchase.class);
        verify(purchaseRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getTotalAmount())
                .isEqualByComparingTo(new BigDecimal("4"));
    }

    @Test
    void createCheckout_forwardsIdempotencyKeyToGateway() throws Exception {
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setPlayerId(PLAYER_ID);
        request.setItems(List.of(
                new PaymentItemDto("pkg", "Coins", 1, BigDecimal.ONE)));

        when(paymentGateway.createPreference(eq(request), anyString(), eq("idem-1")))
                .thenReturn(new PaymentPreferenceResult("id", "pref", "url"));
        when(objectMapper.writeValueAsString(any())).thenReturn("[]");

        paymentService.createCheckout(request, "idem-1");

        // The client key is forwarded to the gateway (which uses it as X-Idempotency-Key).
        verify(paymentGateway).createPreference(eq(request), anyString(), eq("idem-1"));
    }

    // ───────── getPurchaseStatus ─────────

    @Test
    void getPurchaseStatus_ExistingPurchase_ReturnsStatus() {
        purchase.setApproved(true);
        purchase.setRejected(false);
        purchase.setCoinsToAdd(1000);
        when(purchaseRepository.findById(PURCHASE_ID)).thenReturn(Optional.of(purchase));

        Optional<PurchaseStatusResponse> result = paymentService.getPurchaseStatus(PURCHASE_ID);

        assertThat(result).isPresent();
        assertThat(result.get().isApproved()).isTrue();
        assertThat(result.get().isRejected()).isFalse();
        assertThat(result.get().getCoinsToAdd()).isEqualTo(1000);
    }

    @Test
    void getPurchaseStatus_NonExistingPurchase_ReturnsEmpty() {
        when(purchaseRepository.findById("unknown")).thenReturn(Optional.empty());

        Optional<PurchaseStatusResponse> result = paymentService.getPurchaseStatus("unknown");

        assertThat(result).isEmpty();
    }

    @Test
    void getPurchaseStatus_RejectedPurchase_ReturnsRejected() {
        purchase.setApproved(false);
        purchase.setRejected(true);
        when(purchaseRepository.findById(PURCHASE_ID)).thenReturn(Optional.of(purchase));

        Optional<PurchaseStatusResponse> result = paymentService.getPurchaseStatus(PURCHASE_ID);

        assertThat(result).isPresent();
        assertThat(result.get().isRejected()).isTrue();
        assertThat(result.get().isApproved()).isFalse();
    }

    // ───────── confirmPayment ─────────

    @Test
    void confirmPayment_approved_creditsCoinsAndReturnsStatus() {
        when(paymentGateway.processPayment(MP_PAYMENT_ID)).thenReturn(approved());
        when(purchaseRepository.findByIdForUpdate(PURCHASE_ID)).thenReturn(Optional.of(purchase));
        when(playerRepository.findById(PLAYER_ID)).thenReturn(Optional.of(player));

        Optional<PurchaseStatusResponse> result = paymentService.confirmPayment(MP_PAYMENT_ID, PLAYER_ID);

        assertThat(result).isPresent();
        assertThat(result.get().isApproved()).isTrue();
        assertThat(result.get().getCoinsToAdd()).isEqualTo(1000);
        assertThat(player.getCoins()).isEqualTo(1500);
        assertThat(purchase.isCoinsCredited()).isTrue();
    }

    @Test
    void confirmPayment_nullPurchaseId_returnsEmpty() {
        PaymentNotificationResult result = new PaymentNotificationResult(null, true, false, "approved");
        when(paymentGateway.processPayment(MP_PAYMENT_ID)).thenReturn(result);

        assertThat(paymentService.confirmPayment(MP_PAYMENT_ID, PLAYER_ID)).isEmpty();
        verify(purchaseRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void confirmPayment_purchaseNotFound_returnsEmpty() {
        when(paymentGateway.processPayment(MP_PAYMENT_ID)).thenReturn(approved());
        when(purchaseRepository.findByIdForUpdate(PURCHASE_ID)).thenReturn(Optional.empty());

        assertThat(paymentService.confirmPayment(MP_PAYMENT_ID, PLAYER_ID)).isEmpty();
        verify(playerRepository, never()).save(any());
    }

    @Test
    void confirmPayment_purchaseBelongsToAnotherPlayer_throwsForbidden() {
        when(paymentGateway.processPayment(MP_PAYMENT_ID)).thenReturn(approved());
        when(purchaseRepository.findByIdForUpdate(PURCHASE_ID)).thenReturn(Optional.of(purchase));

        assertThatThrownBy(() -> paymentService.confirmPayment(MP_PAYMENT_ID, "intruder"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Purchase does not belong to player");

        verify(playerRepository, never()).save(any());
    }

    // ───────── createCheckout — serialization failure ─────────

    @Test
    void createCheckout_itemsSerializationFails_stillCompletes() throws Exception {
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setPlayerId(PLAYER_ID);
        request.setItems(List.of(new PaymentItemDto("pkg", "Coins", 1, BigDecimal.ONE)));

        when(paymentGateway.createPreference(any(), anyString(), any()))
                .thenReturn(new PaymentPreferenceResult("id", "pref", "url"));
        when(objectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("boom") { });

        PaymentPreferenceResult result = paymentService.createCheckout(request, null);

        assertThat(result.getPreferenceId()).isEqualTo("pref");
        ArgumentCaptor<Purchase> captor = ArgumentCaptor.forClass(Purchase.class);
        verify(purchaseRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getItemsJson()).isNull();
    }
}
