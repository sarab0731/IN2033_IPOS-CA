package domain;

public class RestockOrderItem {
    private final int restockOrderItemId;
    private final int restockOrderId;
    private final String itemId;
    private final String description;
    private final int quantity;
    private final double unitCost;
    private final double lineTotal;

    public RestockOrderItem(int restockOrderItemId, int restockOrderId,
                            String itemId, String description,
                            int quantity, double unitCost, double lineTotal) {
        this.restockOrderItemId = restockOrderItemId;
        this.restockOrderId = restockOrderId;
        this.itemId = itemId;
        this.description = description;
        this.quantity = quantity;
        this.unitCost = unitCost;
        this.lineTotal = lineTotal;
    }

    public int getRestockOrderItemId() { return restockOrderItemId; }
    public int getRestockOrderId()     { return restockOrderId; }
    public String getItemId()          { return itemId; }
    public String getDescription()     { return description; }
    public int getQuantity()           { return quantity; }
    public double getUnitCost()        { return unitCost; }
    public double getLineTotal()       { return lineTotal; }
}
