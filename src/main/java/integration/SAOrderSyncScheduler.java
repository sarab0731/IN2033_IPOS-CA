package integration;

import database.RestockOrderDB;
import domain.RestockOrder;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Periodically polls SA for linked order statuses and advances local CA orders.
 * This ensures delivered supplier orders automatically update CA stock.
 */
public class SAOrderSyncScheduler {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static final int SYNC_INTERVAL_SECONDS = 2;

    public void start() {
        System.out.println("[SAOrderSyncScheduler] Starting periodic SA order sync every " + SYNC_INTERVAL_SECONDS + " seconds");
        scheduler.scheduleAtFixedRate(this::syncOrders, 0, SYNC_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    public void stop() {
        System.out.println("[SAOrderSyncScheduler] Stopping scheduler");
        scheduler.shutdown();
    }

    private void syncOrders() {
        try {
            List<RestockOrder> activeOrders = RestockOrderDB.getActiveSAOrders();
            if (activeOrders.isEmpty()) return;

            int advanced = 0;
            for (RestockOrder order : activeOrders) {
                String saOrderId = RestockOrderDB.getSAOrderId(order.getRestockOrderId());
                if (saOrderId == null || saOrderId.isBlank()) continue;

                String tracking = SAApiClient.trackDelivery(saOrderId);
                String saStatus = SASync.parseTrackedOrderStatus(tracking);
                String mappedStatus = SASync.mapSAStatusToCA(saStatus);

                if (mappedStatus != null && SASync.isStatusProgression(order.getStatus(), mappedStatus)) {
                    boolean updated = RestockOrderDB.updateStatus(order.getRestockOrderId(), order.getStatus(), mappedStatus);
                    if (updated) {
                        advanced++;
                        System.out.println("[SAOrderSyncScheduler] Synced " + order.getOrderNumber()
                                + " from " + order.getStatus() + " to " + mappedStatus
                                + " using SA order " + saOrderId);
                    }
                }
            }

            if (advanced > 0) {
                System.out.println("[SAOrderSyncScheduler] Advanced " + advanced + " local order(s) from SA status updates.");
            }
        } catch (Exception e) {
            System.err.println("[SAOrderSyncScheduler] Sync failed: " + e.getMessage());
        }
    }
}
