package integration;

import database.DatabaseManager;
import database.ProductDB;
import domain.Product;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

/**
 * Handles synchronization between CA and PU systems.
 * - On CA startup: pulls pending stock changes from PU and applies them
 * - Pushes CA's product catalog to PU cache
 */
public class PUSync {

    /**
     * Called on CA startup to sync with PU.
     * 1. Fetch pending stock changes from PU
     * 2. Apply them to CA's database
     * 3. Clear pending changes in PU
     * 4. Push CA's current product catalog to PU cache
     */
    public static void syncWithPU() {
        System.out.println("[PUSync] Starting sync with PU system...");
        
        // Step 1: Get pending changes from PU
        JSONArray pendingChanges = PUApiClient.getPendingStockChanges();
        
        if (pendingChanges.length() > 0) {
            System.out.println("[PUSync] Found " + pendingChanges.length() + " pending stock changes from PU.");
            
            // Step 2: Apply changes to CA
            int applied = applyStockChanges(pendingChanges);
            System.out.println("[PUSync] Applied " + applied + " stock changes to CA database.");
            
            // Step 3: Clear pending changes in PU
            if (applied > 0) {
                boolean cleared = PUApiClient.clearAllPendingChanges();
                if (cleared) {
                    System.out.println("[PUSync] Cleared pending changes in PU.");
                } else {
                    System.err.println("[PUSync] Warning: Failed to clear pending changes in PU.");
                }
            }
        } else {
            System.out.println("[PUSync] No pending stock changes from PU.");
        }
        
        // Step 4: Push CA products to PU cache
        pushProductsToPU();
        
        System.out.println("[PUSync] Sync complete.");
    }

    /**
     * Applies pending stock changes from PU to CA's database.
     * pendingChange is negative for stock deductions (e.g., -5 means reduce by 5).
     */
    private static int applyStockChanges(JSONArray changes) {
        int applied = 0;
        String sql = "UPDATE products SET stock_quantity = stock_quantity + ? WHERE product_id = ?";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            for (int i = 0; i < changes.length(); i++) {
                JSONObject change = changes.getJSONObject(i);
                int productId = change.getInt("productId");
                int pendingChange = change.getInt("pendingChange");
                
                stmt.setInt(1, pendingChange);  // pendingChange is already negative for deductions
                stmt.setInt(2, productId);
                stmt.executeUpdate();
                applied++;
                
                System.out.println("[PUSync] Applied stock change: product " + productId + " change " + pendingChange);
            }
        } catch (Exception e) {
            System.err.println("[PUSync] Error applying stock changes: " + e.getMessage());
            e.printStackTrace();
        }
        
        return applied;
    }

    /**
     * Pushes CA's current product catalog to PU cache.
     */
    private static void pushProductsToPU() {
        List<Product> products = ProductDB.getAllProducts();
        
        if (products.isEmpty()) {
            System.out.println("[PUSync] No products to push to PU.");
            return;
        }
        
        JSONArray arr = new JSONArray();
        for (Product p : products) {
            JSONObject obj = new JSONObject();
            obj.put("productId", p.getProductId());
            obj.put("itemId", p.getItemId());
            obj.put("description", p.getDescription());
            obj.put("packageType", p.getPackageType() != null ? p.getPackageType() : "");
            obj.put("unitsInPack", p.getUnitsInPack());
            obj.put("price", p.getPrice());
            obj.put("vatRate", p.getVatRate());
            obj.put("stockQuantity", p.getStockQuantity());
            obj.put("minStockLevel", p.getMinStockLevel());
            obj.put("isActive", 1);
            arr.put(obj);
        }
        
        boolean pushed = PUApiClient.pushProductsToCache(arr.toString());
        if (pushed) {
            System.out.println("[PUSync] Pushed " + products.size() + " products to PU cache.");
        } else {
            System.err.println("[PUSync] Warning: Failed to push products to PU cache (PU may be offline).");
        }
    }
}
