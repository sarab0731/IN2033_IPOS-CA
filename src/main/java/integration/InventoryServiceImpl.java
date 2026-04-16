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

/**
 * Implements {@link IInventoryService} for the SA subsystem.
 * SA uses this to read CA's catalogue and to confirm stock deductions
 * after a delivery has been dispatched.
 */
public class InventoryServiceImpl implements IInventoryService {

    /**
     * Returns the full CA product catalogue as a list of {@link CatalogueItem}s.
     * SA uses this to display what CA stocks and at what prices.
     */
    @Override
    public List<CatalogueItem> getCatalogue() {
        return ProductDB.getAllProducts().stream()
                .map(ProductMapper::toCatalogueItem)
                .collect(Collectors.toList());
    }

    /**
     * Deducts one unit of the given item from CA stock.
     * Called by SA when a delivery to CA is confirmed.
     *
     * @param itemID the CA item ID (matches {@code item_id} in the products table)
     * @return "OK" on success, or an "ERROR: ..." message if the item is not found,
     *         has no stock, or the database update fails
     */
    @Override
    public String deductStock(String itemID) {
        Product product = ProductDB.getByItemId(itemID);
        if (product == null)              return "ERROR: Item not found - " + itemID;
        if (product.getStockQuantity() <= 0) return "ERROR: Out of stock - " + itemID;

        String sql = "UPDATE products SET stock_quantity = stock_quantity - 1 " +
                     "WHERE item_id = ? AND stock_quantity >= 1";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, itemID);
            int rows = stmt.executeUpdate();
            if (rows == 0) return "ERROR: Stock update failed (race condition) for " + itemID;
            return "OK";

        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR: " + e.getMessage();
        }
    }
}
