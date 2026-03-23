package domain;

public class Product {

    private int productId;
    private String itemId;
    private String description;
    private String packageType;
    private int unitsInPack;
    private double price;
    private double vatRate;
    private int stockQuantity;
    private int minStockLevel;

    public Product(int productId, String itemId, String description, String packageType,
                   int unitsInPack, double price, double vatRate,
                   int stockQuantity, int minStockLevel) {
        this.productId     = productId;
        this.itemId        = itemId;
        this.description   = description;
        this.packageType   = packageType;
        this.unitsInPack   = unitsInPack;
        this.price         = price;
        this.vatRate       = vatRate;
        this.stockQuantity = stockQuantity;
        this.minStockLevel = minStockLevel;
    }

    public int    getProductId()     { return productId; }
    public String getItemId()        { return itemId; }
    public String getDescription()   { return description; }
    public String getPackageType()   { return packageType; }
    public int    getUnitsInPack()   { return unitsInPack; }
    public double getPrice()         { return price; }
    public double getVatRate()       { return vatRate; }
    public int    getStockQuantity() { return stockQuantity; }
    public int    getMinStockLevel() { return minStockLevel; }

    public boolean isLowStock() {
        return stockQuantity <= minStockLevel;
    }

    public void setDescription(String description)   { this.description = description; }
    public void setPrice(double price)               { this.price = price; }
    public void setStockQuantity(int stockQuantity)  { this.stockQuantity = stockQuantity; }
    public void setMinStockLevel(int minStockLevel)  { this.minStockLevel = minStockLevel; }
    public void setVatRate(double vatRate)           { this.vatRate = vatRate; }
    public void setPackageType(String packageType)   { this.packageType = packageType; }
    public void setUnitsInPack(int unitsInPack)      { this.unitsInPack = unitsInPack; }

    @Override
    public String toString() {
        return itemId + " - " + description;
    }
}