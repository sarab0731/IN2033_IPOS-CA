package integration;

import integration.interfaces.IOrderService;
import domain.DiscountPlan;
import org.json.JSONArray;

/**
 * Implements {@link IOrderService} by delegating to {@link SAApiClient}.
 * Handles restock orders, delivery tracking, and merchant financial queries
 * against the SA subsystem.
 */
public class OrderServiceImpl implements IOrderService {

    /**
     * Places a restock order via SA.
     *
     * @param merchantID   the merchant placing the order
     * @param orderDetails JSON string containing an "items" array, each with itemId + quantity
     * @return the SA-assigned order ID, or an error message on failure
     */
    @Override
    public String placeRestockOrder(String merchantID, String orderDetails) {
        try {
            JSONArray items = new JSONArray(orderDetails);
            String orderId = SAApiClient.placeOrder(merchantID, items);
            return orderId != null ? orderId : "ERROR: SA rejected the order";
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    /**
     * Tracks the delivery status of an SA order.
     *
     * @param orderID the SA order ID
     * @return a tracking status string, or an empty string if unavailable
     */
    @Override
    public String trackDelivery(String orderID) {
        return SAApiClient.trackDelivery(orderID);
    }

    /**
     * Returns the outstanding balance owed by a merchant to SA.
     *
     * @param merchantID the merchant ID
     * @return balance amount, or -1 if SA is unreachable
     */
    @Override
    public double queryOutstandingBalance(String merchantID) {
        return SAApiClient.getBalance(merchantID);
    }

    /**
     * Retrieves an invoice for an SA order.
     *
     * @param orderID the SA order ID
     * @return invoice string, or an empty string if unavailable
     */
    @Override
    public String getInvoice(String orderID) {
        return SAApiClient.getInvoice(orderID);
    }

    /**
     * Checks whether a merchant's SA account matches a given status.
     *
     * @param merchantID the merchant ID
     * @param status     the status to check (e.g. "normal", "suspended", "in_default")
     * @return true if SA confirms the merchant has that status
     */
    @Override
    public boolean getAccStatus(String merchantID, String status) {
        return SAApiClient.checkAccountStatus(merchantID, status);
    }

    /**
     * Checks whether a merchant is enrolled in the given discount plan via SA.
     *
     * @param merchantID the merchant ID
     * @param plan       the discount plan to check
     * @return true if the plan is non-null (plan membership is managed locally)
     */
    @Override
    public boolean viewDiscountPlan(String merchantID, DiscountPlan plan) {
        return plan != null;
    }
}
