package domain;

import java.util.List;

public class PUOrder {

    private final String orderId;
    private final String memberName;
    private final String deliveryAddress;
    private final String status;
    private final double totalValue;
    private final List<PUOrderItem> items;

    public PUOrder(String orderId, String memberName, String deliveryAddress,
                   String status, double totalValue, List<PUOrderItem> items) {
        this.orderId         = orderId;
        this.memberName      = memberName;
        this.deliveryAddress = deliveryAddress;
        this.status          = status;
        this.totalValue      = totalValue;
        this.items           = items;
    }

    public String getOrderId()         { return orderId; }
    public String getMemberName()      { return memberName; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public String getStatus()          { return status; }
    public double getTotalValue()      { return totalValue; }
    public List<PUOrderItem> getItems(){ return items; }

    @Override
    public String toString() { return orderId + " — " + memberName; }


    public static class PUOrderItem {
        private final String productName;
        private final int quantity;
        private final double unitPrice;

        public PUOrderItem(String productName, int quantity, double unitPrice) {
            this.productName = productName;
            this.quantity    = quantity;
            this.unitPrice   = unitPrice;
        }

        public String getProductName() { return productName; }
        public int    getQuantity()    { return quantity; }
        public double getUnitPrice()   { return unitPrice; }
    }
}
