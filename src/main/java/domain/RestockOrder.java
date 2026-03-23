package domain;

public class RestockOrder {

    private int restockOrderId;
    private String orderNumber;
    private String merchantId;
    private String status;
    private double totalValue;
    private String createdAt;

    public RestockOrder(int restockOrderId, String orderNumber, String merchantId,
                        String status, double totalValue, String createdAt) {
        this.restockOrderId = restockOrderId;
        this.orderNumber    = orderNumber;
        this.merchantId     = merchantId;
        this.status         = status;
        this.totalValue     = totalValue;
        this.createdAt      = createdAt;
    }

    public int    getRestockOrderId() { return restockOrderId; }
    public String getOrderNumber()    { return orderNumber; }
    public String getMerchantId()     { return merchantId; }
    public String getStatus()         { return status; }
    public double getTotalValue()     { return totalValue; }
    public String getCreatedAt()      { return createdAt; }

    @Override
    public String toString() {
        return orderNumber + " | " + status + " | £" + String.format("%.2f", totalValue);
    }
}