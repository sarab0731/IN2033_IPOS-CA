package domain;

public class CatalogueItem {

    private String itemId;
    private String description;
    private String packageType;
    private int unitsInPack;
    private double price;
    private int availability;

    public CatalogueItem() {}

    public String getItemId()       { return itemId; }
    public String getDescription()  { return description; }
    public String getPackageType()  { return packageType; }
    public int    getUnitsInPack()  { return unitsInPack; }
    public double getPrice()        { return price; }
    public int    getAvailability() { return availability; }

    public void setItemId(String itemId)           { this.itemId = itemId; }
    public void setDescription(String description) { this.description = description; }
    public void setPackageType(String packageType) { this.packageType = packageType; }
    public void setUnitsInPack(int unitsInPack)    { this.unitsInPack = unitsInPack; }
    public void setPrice(double price)             { this.price = price; }
    public void setAvailability(int availability)  { this.availability = availability; }

    @Override
    public String toString() {
        return itemId + " - " + description + " | £" + String.format("%.2f", price)
                + " | Stock: " + availability;
    }
}