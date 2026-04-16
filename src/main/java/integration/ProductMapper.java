package integration;

import domain.CatalogueItem;
import domain.Product;
import org.json.JSONObject;

/**
 * Converts {@link Product} domain objects into the representations used by
 * external subsystems — either a {@link CatalogueItem} (for SA/PU service
 * interfaces) or a {@link JSONObject} (for PU cache sync payloads).
 */
class ProductMapper {

    private ProductMapper() {}

    /**
     * Maps a CA {@link Product} to a {@link CatalogueItem} for use by the
     * SA and PU integration interfaces.
     */
    static CatalogueItem toCatalogueItem(Product p) {
        CatalogueItem item = new CatalogueItem();
        item.setItemId(p.getItemId());
        item.setDescription(p.getDescription());
        item.setPackageType(p.getPackageType());
        item.setUnitsInPack(p.getUnitsInPack());
        item.setPrice(p.getPrice());
        item.setAvailability(p.getStockQuantity());
        return item;
    }

    /**
     * Serialises a CA {@link Product} to a {@link JSONObject} for the PU cache
     * sync endpoint ({@code /api/sync/update-cache}).
     */
    static JSONObject toJson(Product p) {
        return new JSONObject()
                .put("productId",    p.getProductId())
                .put("itemId",       p.getItemId())
                .put("description",  p.getDescription())
                .put("packageType",  p.getPackageType() != null ? p.getPackageType() : "")
                .put("unitsInPack",  p.getUnitsInPack())
                .put("price",        p.getPrice())
                .put("vatRate",      p.getVatRate())
                .put("stockQuantity", p.getStockQuantity())
                .put("minStockLevel", p.getMinStockLevel())
                .put("isActive",     1);
    }
}
