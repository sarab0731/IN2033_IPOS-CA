package integration;

// AI-assisted: used Claude to learn how to schedule a background task with
// ScheduledExecutorService to periodically sync product data.

import database.ProductDB;
import domain.Product;
import org.json.JSONArray;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Scheduled task to push CA products to PU cache every 30 seconds.
 * Logs a single warning when PU goes offline and a single notice when it comes back,
 * rather than repeating the failure message on every cycle.
 */
public class ProductSyncScheduler {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static final int SYNC_INTERVAL_SECONDS = 30;

    /** Tracks whether PU was reachable on the previous cycle to suppress repeated warnings. */
    private boolean puReachable = true;

    /**
     * Starts the periodic sync with PU.
     */
    public void start() {
        System.out.println("[ProductSyncScheduler] Starting periodic sync every " + SYNC_INTERVAL_SECONDS + " seconds");
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
     * Fetches CA products and pushes them to PU cache.
     * Silently skips if PU is already known to be offline (logs only on state change).
     */
    private void syncProducts() {
        try {
            List<Product> products = ProductDB.getAllProducts();
            if (products.isEmpty()) return;

            JSONArray productsJson = new JSONArray();
            for (Product p : products) productsJson.put(ProductMapper.toJson(p));

            boolean success = PUApiClient.pushProductsToCache(productsJson.toString());

            if (success) {
                if (!puReachable) {
                    System.out.println("[ProductSyncScheduler] PU back online — resuming sync.");
                    puReachable = true;
                }
            } else {
                if (puReachable) {
                    System.err.println("[ProductSyncScheduler] PU unreachable — sync paused until PU comes back.");
                    puReachable = false;
                }
            }

        } catch (Exception e) {
            if (puReachable) {
                System.err.println("[ProductSyncScheduler] Sync error: " + e.toString());
                puReachable = false;
            }
        }
    }
}
