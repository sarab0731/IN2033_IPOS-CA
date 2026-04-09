package integration;

import database.ProductDB;
import domain.Product;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Scheduled task to push CA products to PU cache every 30 seconds.
 */
public class ProductSyncScheduler {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static final int SYNC_INTERVAL_SECONDS = 30;

    /**
     * Starts the periodic sync with PU.
     */
    public void start() {
        System.out.println("[ProductSyncScheduler] Starting periodic sync every " + SYNC_INTERVAL_SECONDS + " seconds");
        // Initial delay = interval so pull completes before first push
        scheduler.scheduleAtFixedRate(this::syncProducts, SYNC_INTERVAL_SECONDS, SYNC_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Stops the scheduler.
     */
    public void stop() {
        System.out.println("[ProductSyncScheduler] Stopping scheduler");
        scheduler.shutdown();
    }

    /**
     * Performs the actual sync: fetches CA products and pushes to PU.
     */
    private void syncProducts() {
        try {
            // 1. Fetch all products from CA database
            List<Product> products = ProductDB.getAllProducts();
            
            if (products.isEmpty()) {
                System.out.println("[ProductSyncScheduler] No products to sync");
                return;
            }

            // 2. Convert to JSON array with all fields
            JSONArray productsJson = new JSONArray();
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
                obj.put("isActive", 1); // Only active products are synced
                productsJson.put(obj);
                
                // Log each product being synced
                System.out.println("[ProductSyncScheduler] Preparing product: " + p.getItemId() + 
                    " - stock=" + p.getStockQuantity() + ", price=" + p.getPrice());
            }

            // 3. Push to PU cache
            String jsonString = productsJson.toString();
            System.out.println("[ProductSyncScheduler] Sending " + products.size() + " products to PU, JSON size: " + jsonString.length() + " bytes");
            
            boolean success = PUApiClient.pushProductsToCache(jsonString);
            
            if (success) {
                System.out.println("[ProductSyncScheduler] Successfully synced " + products.size() + " products to PU cache");
            } else {
                System.err.println("[ProductSyncScheduler] Failed to sync products to PU");
            }

        } catch (Exception e) {
            System.err.println("[ProductSyncScheduler] Error during sync: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
