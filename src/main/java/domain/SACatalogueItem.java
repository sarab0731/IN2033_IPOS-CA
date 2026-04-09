package domain;

/**
 * Represents a product from the InfoPharma SA (Supplier) catalogue.
 * This is distinct from the local CA stock (Product class).
 * In a full integration, this would be fetched from the InfoPharma API.
 */
public class SACatalogueItem {

    private final String itemId;
    private final String description;
    private final String category;
    private final double unitCost;
    private final int packSize;
    private final String manufacturer;

    public SACatalogueItem(String itemId, String description, String category,
                           double unitCost, int packSize, String manufacturer) {
        this.itemId       = itemId;
        this.description  = description;
        this.category     = category;
        this.unitCost     = unitCost;
        this.packSize     = packSize;
        this.manufacturer = manufacturer;
    }

    public String getItemId()       { return itemId; }
    public String getDescription()  { return description; }
    public String getCategory()     { return category; }
    public double getUnitCost()     { return unitCost; }
    public int    getPackSize()     { return packSize; }
    public String getManufacturer() { return manufacturer; }

    @Override
    public String toString() {
        return description + " (" + itemId + ")";
    }
}