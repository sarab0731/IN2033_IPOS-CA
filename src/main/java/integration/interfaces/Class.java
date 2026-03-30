package integration.interfaces;

import integration.InventoryServiceImpl;
import integration.OrderStatusImpl;
import integration.StockServiceImpl;

public class Class {

    public static IStockService getStockService() {
        return new StockServiceImpl();
    }

    public static IInventoryService getInventoryService() {
        return new InventoryServiceImpl();
    }

    public static IOrderStatus getOrderStatus() {
        return new OrderStatusImpl();
    }

    public static IOrderService getOrderService() {
        return new integration.Class.OrderServiceImpl();
    }

    public static IPaymentProcessing getPaymentProcessing() {
        return new integration.Class.PaymentProcessingImpl();
    }

    public static IEmailService getEmailService() {
        return new integration.Class.EmailServiceImpl();
    }

    public static ISMTPConnection getSMTPConnection() {
        return new integration.Class.SMTPConnectionImpl();
    }

    public static ICommercialMembershipService getCommercialMembershipService() {
        return new integration.Class.CommercialMembershipServiceImpl();
    }
}
