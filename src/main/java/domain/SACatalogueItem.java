package domain;

/**
 * Represents a product from the InfoPharma SA catalogue.
 * SA API provides: itemId, description, packageCost, availability.
 * Category and manufacturer are not supplied by SA.
 */
public class SACatalogueItem {

    private final String itemId;
    private final String description;
    private final String category;     // always empty — SA does not send category
    private final double unitCost;
    private final int packSize;        // always 1 — SA does not send pack size
    private final String manufacturer; // always empty — SA does not send manufacturer
    private final int availability;    // packs available at SA

    public SACatalogueItem(String itemId, String description, String category,
                           double unitCost, int packSize, String manufacturer) {
        this(itemId, description, category, unitCost, packSize, manufacturer, 0);
    }

    public SACatalogueItem(String itemId, String description, String category,
                           double unitCost, int packSize, String manufacturer, int availability) {
        this.itemId       = itemId;
        this.description  = description;
        this.category     = category;
        this.unitCost     = unitCost;
        this.packSize     = packSize;
        this.manufacturer = manufacturer;
        this.availability = availability;
    }

    public String getItemId()       { return itemId; }
    public String getDescription()  { return description; }
    public String getCategory()     { return category; }
    public double getUnitCost()     { return unitCost; }
    public int    getPackSize()     { return packSize; }
    public String getManufacturer() { return manufacturer; }
    public int    getAvailability() { return availability; }

    @Override
    public String toString() {
        return description + " (" + itemId + ")";
    }
}
