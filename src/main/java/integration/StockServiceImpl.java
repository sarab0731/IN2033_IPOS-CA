package integration;

import database.DatabaseManager;
import database.ProductDB;
import domain.CatalogueItem;
import domain.Product;
import integration.interfaces.IStockService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.stream.Collectors;

public class StockServiceImpl implements IStockService {

    /**
     * PU calls this to get full details of a specific item by item ID.
     */
    @Override
    public CatalogueItem itemAvailability(String itemID) {
        return ProductDB.getAllProducts().stream()
                .filter(p -> p.getItemId().equals(itemID))
                .findFirst()
                .map(this::toCatalogueItem)
                .orElse(null);
    }

    /**
     * PU calls this to search stock by keyword.
     */
    @Override
    public List<CatalogueItem> searchStock(String keyword) {
        String lower = keyword.toLowerCase();
        return ProductDB.getAllProducts().stream()
                .filter(p -> p.getDescription().toLowerCase().contains(lower)
                        || p.getItemId().toLowerCase().contains(lower))
                .map(this::toCatalogueItem)
                .collect(Collectors.toList());
    }

    /**
     * PU calls this after a confirmed online sale to deduct stock from CA.
     */
    @Override
    public String deductStock(String itemID) {
        Product product = ProductDB.getAllProducts().stream()
                .filter(p -> p.getItemId().equals(itemID))
                .findFirst()
                .orElse(null);

        if (product == null) return "ERROR: Item not found - " + itemID;
        if (product.getStockQuantity() <= 0) return "ERROR: Out of stock - " + itemID;

        String sql = "UPDATE products SET stock_quantity = stock_quantity - 1 WHERE item_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, itemID);
            stmt.executeUpdate();
            return "OK: Stock deducted for " + itemID;

        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR: " + e.getMessage();
        }
    }

    /**
     * Returns full catalogue for PU to display.
     */
    @Override
    public List<CatalogueItem> getCatalogue() {
        return ProductDB.getAllProducts().stream()
                .map(this::toCatalogueItem)
                .collect(Collectors.toList());
    }

    // maps Product → CatalogueItem (the shared domain object)
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