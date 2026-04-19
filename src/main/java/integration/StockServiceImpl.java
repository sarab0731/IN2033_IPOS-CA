package integration;

// AI-assisted: used Claude to learn how to implement the stock service and keep
// local stock levels in sync with what the SA subsystem exposes.

import database.DatabaseManager;
import database.ProductDB;
import domain.CatalogueItem;
import domain.Product;
import integration.interfaces.IStockService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implements {@link IStockService} for the PU subsystem.
 * PU uses this to display CA stock to online shoppers, search for items,
 * and confirm deductions after an online sale has been fulfilled.
 */
public class StockServiceImpl implements IStockService {

    /**
     * Returns full details for a single item by its item ID.
     * PU uses this to show a product detail page.
     *
     * @param itemID the CA item ID
     * @return the matching {@link CatalogueItem}, or {@code null} if not found
     */
    @Override
    public CatalogueItem itemAvailability(String itemID) {
        Product product = ProductDB.getByItemId(itemID);
        return product != null ? ProductMapper.toCatalogueItem(product) : null;
    }

    /**
     * Searches CA stock by keyword against item ID and description.
     * PU uses this for its product search screen.
     *
     * @param keyword case-insensitive search term
     * @return list of matching {@link CatalogueItem}s (may be empty, never null)
     */
    @Override
    public List<CatalogueItem> searchStock(String keyword) {
        return ProductDB.searchProducts(keyword).stream()
                .map(ProductMapper::toCatalogueItem)
                .collect(Collectors.toList());
    }

    /**
     * Deducts one unit of the given item from CA stock after an online sale.
     * PU calls this when a confirmed order line is dispatched.
     *
     * @param itemID the CA item ID
     * @return "OK: Stock deducted for &lt;itemID&gt;" on success,
     *         or an "ERROR: ..." message if the item is missing, out of stock,
     *         or the update fails
     */
    @Override
    public String deductStock(String itemID) {
        Product product = ProductDB.getByItemId(itemID);
        if (product == null)                 return "ERROR: Item not found - " + itemID;
        if (product.getStockQuantity() <= 0) return "ERROR: Out of stock - " + itemID;

        String sql = "UPDATE products SET stock_quantity = stock_quantity - 1 " +
                     "WHERE item_id = ? AND stock_quantity >= 1";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, itemID);
            int rows = stmt.executeUpdate();
            if (rows == 0) return "ERROR: Stock update failed (race condition) for " + itemID;
            return "OK: Stock deducted for " + itemID;

        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR: " + e.getMessage();
        }
    }

    /**
     * Returns the full CA product catalogue.
     * PU uses this to populate its product listing.
     */
    @Override
    public List<CatalogueItem> getCatalogue() {
        return ProductDB.getAllProducts().stream()
                .map(ProductMapper::toCatalogueItem)
                .collect(Collectors.toList());
    }
}
