package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.payment.CreatePaymentRequest;
import ar.edu.utn.frc.tup.piii.dtos.payment.PaymentNotificationResult;
import ar.edu.utn.frc.tup.piii.dtos.payment.PaymentPreferenceResult;

/**
 * Internal port for payment gateway adapters.
 *
 * <p>Implement this interface to add a new payment provider (e.g. PayPal, Stripe).
 * The system depends only on this interface; concrete adapters live in
 * {@code services.adapters} and are swapped via Spring's DI.
 */
public interface PaymentGateway {

    /**
     * Creates a payment preference and returns the redirect URL for the buyer.
     *
     * @param request        cart items and buyer context
     * @param purchaseId     internal purchase ID used as external reference
     * @param idempotencyKey client-supplied idempotency key; forwarded to the provider
     *                       (e.g. MercadoPago's {@code X-Idempotency-Key}) so retries do
     *                       not create duplicate preferences. May be null.
     * @return result containing the checkout URL and provider preference ID
     */
    PaymentPreferenceResult createPreference(CreatePaymentRequest request, String purchaseId,
            String idempotencyKey);

    /**
     * Fetches payment status from the provider and maps it to approved/rejected.
     *
     * @param mpPaymentId the provider's payment identifier received via webhook
     * @return result with {@code approved=true} or {@code rejected=true}
     */
    PaymentNotificationResult processPayment(String mpPaymentId);
}
