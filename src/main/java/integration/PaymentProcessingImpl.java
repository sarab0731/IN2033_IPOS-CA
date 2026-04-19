package integration;

// AI-assisted: used Claude to learn how to implement a payment processing
// service and integrate it with the external payment API endpoint.

import integration.interfaces.IPaymentProcessing;

/**
 * Implements {@link IPaymentProcessing} by delegating to the PU payment gateway
 * via {@link PUApiClient}.
 */
public class PaymentProcessingImpl implements IPaymentProcessing {

    /**
     * Processes a card payment via PU's payment gateway.
     *
     * @param merchantID  the merchant requesting the payment
     * @param orderID     the order being paid for
     * @param fullName    cardholder name
     * @param address     billing address
     * @param cardDetails array of [cardNumber, expiry, cvv, (unused extra fields ignored)]
     * @param amount      the amount to charge
     * @return "OK: Payment processed" on success, or an error message on failure
     */
    @Override
    public String requestPayment(String merchantID, String orderID, String fullName,
                                 String address, String[] cardDetails, double amount) {
        if (cardDetails == null || cardDetails.length < 3) {
            return "ERROR: Invalid card details";
        }
        if (amount <= 0) {
            return "ERROR: Invalid amount";
        }

        String result = PUApiClient.processCardPayment(
                cardDetails[0], cardDetails[1], cardDetails[2], amount);

        return switch (result) {
            case "success"  -> "OK: Payment of £" + String.format("%.2f", amount)
                                   + " processed for order " + orderID;
            case "declined" -> "ERROR: Card declined";
            default         -> "ERROR: Payment gateway unreachable";
        };
    }
}
