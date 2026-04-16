package integration;

import database.ProductDB;
import domain.Product;
import org.json.JSONArray;

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

            // 2. Convert to JSON array
            JSONArray productsJson = new JSONArray();
            for (Product p : products) productsJson.put(ProductMapper.toJson(p));

            // 3. Push to PU cache
            boolean success = PUApiClient.pushProductsToCache(productsJson.toString());
            if (success) {
                System.out.println("[ProductSyncScheduler] Synced " + products.size() + " products to PU.");
            }

        } catch (Exception e) {
            System.err.println("[ProductSyncScheduler] Sync failed: " + e.getMessage());
        }
    }
}
