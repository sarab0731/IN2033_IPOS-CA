package integration.interfaces;

import integration.CommercialMembershipServiceImpl;
import integration.EmailServiceImpl;
import integration.InventoryServiceImpl;
import integration.OrderServiceImpl;
import integration.OrderStatusImpl;
import integration.PaymentProcessingImpl;
import integration.SMTPConnectionImpl;
import integration.StockServiceImpl;

/**
 * Factory that provides the canonical implementation for each integration interface.
 *
 * <p>Callers obtain a typed service instance without depending on a concrete class:
 * <pre>
 *   IStockService stock = IntegrationServiceFactory.getStockService();
 *   CatalogueItem item  = stock.itemAvailability("A1001");
 * </pre>
 */
public class IntegrationServiceFactory {

    private IntegrationServiceFactory() {}

    /** Returns the CA stock service used by PU (search, availability, deduct). */
    public static IStockService getStockService() {
        return new StockServiceImpl();
    }

    /** Returns the CA inventory service used by SA (catalogue, deduct). */
    public static IInventoryService getInventoryService() {
        return new InventoryServiceImpl();
    }

    /** Returns the order status service for querying restock order state. */
    public static IOrderStatus getOrderStatus() {
        return new OrderStatusImpl();
    }

    /** Returns the SA order service (place, track, balance, invoice). */
    public static IOrderService getOrderService() {
        return new OrderServiceImpl();
    }

    /** Returns the PU payment processing service. */
    public static IPaymentProcessing getPaymentProcessing() {
        return new PaymentProcessingImpl();
    }

    /** Returns the PU email service (rich metadata variant). */
    public static IEmailService getEmailService() {
        return new EmailServiceImpl();
    }

    /** Returns the PU SMTP relay (simple subject/body variant). */
    public static ISMTPConnection getSMTPConnection() {
        return new SMTPConnectionImpl();
    }

    /** Returns the SA commercial membership application service. */
    public static ICommercialMembershipService getCommercialMembershipService() {
        return new CommercialMembershipServiceImpl();
    }
}
