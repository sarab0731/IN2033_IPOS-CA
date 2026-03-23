package integration;

import database.DatabaseManager;
import database.ProductDB;
import domain.CatalogueItem;
import domain.Product;
import integration.interfaces.IInventoryService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.stream.Collectors;

public class InventoryServiceImpl implements IInventoryService {

    /**
     * SA calls this to get the full catalogue from CA.
     */
    @Override
    public List<CatalogueItem> getCatalogue() {
        return ProductDB.getAllProducts().stream()
                .map(this::toCatalogueItem)
                .collect(Collectors.toList());
    }

    /**
     * SA calls this to deduct stock on CA side when a delivery is confirmed.
     */
    @Override
    public String deductStock(String itemID) {
        Product product = ProductDB.getAllProducts().stream()
                .filter(p -> p.getItemId().equals(itemID))
                .findFirst()
                .orElse(null);

        if (product == null) return "ERROR: Item not found - " + itemID;

        String sql = "UPDATE products SET stock_quantity = stock_quantity - 1 WHERE item_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, itemID);
            stmt.executeUpdate();
            return "OK";

        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR: " + e.getMessage();
        }
    }

    private CatalogueItem toCatalogueItem(Product p) {
        CatalogueItem item = new CatalogueItem();
        item.setItemId(p.getItemId());
        item.setDescription(p.getDescription());
        item.setPackageType(p.getPackageType());
        item.setUnitsInPack(p.getUnitsInPack());
        item.setPrice(p.getPrice());
        item.setAvailability(p.getStockQuantity());
        return item;
    }
}