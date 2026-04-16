package integration.interfaces;

/**
 * @deprecated Use {@link IntegrationServiceFactory} instead.
 */
@Deprecated
public class Class {

    public static IStockService getStockService()                       { return IntegrationServiceFactory.getStockService(); }
    public static IInventoryService getInventoryService()               { return IntegrationServiceFactory.getInventoryService(); }
    public static IOrderStatus getOrderStatus()                         { return IntegrationServiceFactory.getOrderStatus(); }
    public static IOrderService getOrderService()                       { return IntegrationServiceFactory.getOrderService(); }
    public static IPaymentProcessing getPaymentProcessing()             { return IntegrationServiceFactory.getPaymentProcessing(); }
    public static IEmailService getEmailService()                       { return IntegrationServiceFactory.getEmailService(); }
    public static ISMTPConnection getSMTPConnection()                   { return IntegrationServiceFactory.getSMTPConnection(); }
    public static ICommercialMembershipService getCommercialMembershipService() { return IntegrationServiceFactory.getCommercialMembershipService(); }
}
