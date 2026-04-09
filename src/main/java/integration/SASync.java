package integration;

import database.DatabaseManager;
import database.ProductDB;
import domain.Product;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

/**
 * Handles synchronization between CA and SA systems.
 *
 * On CA startup:
 *   1. Pull SA's active catalogue and update local SA price/availability cache
 *   2. Verify merchant account statuses are still active in SA
 *   3. Refresh cached discount rates and credit limits from SA
 *
 * Can also be called periodically to keep CA's view of SA data fresh.
 */
public class SASync {

    /**
     * Called on CA startup to sync with the SA system.
     * Runs all sync steps in sequence; each step is independent so a failure
     * in one does not prevent the others from running.
     */
    public static void syncWithSA() {
        System.out.println("[SASync] Starting sync with SA system...");

        // Step 1: Pull SA catalogue into local cache
        syncCatalogue();

        // Step 2: Verify merchant account statuses
        syncMerchantStatuses();

        // Step 3: Refresh discount rates and credit limits
        syncMerchantFinancials();

        System.out.println("[SASync] Sync complete.");
    }

    // ==================== STEP 1: CATALOGUE SYNC ====================

    /**
     * Fetches SA's active catalogue and upserts it into CA's local
     * sa_catalogue_cache table so that CA screens can display SA pricing
     * and availability without hitting SA on every request.
     */
    private static void syncCatalogue() {
        System.out.println("[SASync] Syncing SA catalogue...");

        JSONArray catalogue = SAApiClient.getCatalogue();

        if (catalogue.length() == 0) {
            System.out.println("[SASync] No catalogue items received from SA (SA may be offline).");
            return;
        }

        String upsertSql =
                "INSERT INTO sa_catalogue_cache (item_id, description, package_cost, availability, last_synced) " +
                        "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP()) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "  description = VALUES(description), " +
                        "  package_cost = VALUES(package_cost), " +
                        "  availability = VALUES(availability), " +
                        "  last_synced = CURRENT_TIMESTAMP()";

        int synced = 0;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(upsertSql)) {

            for (int i = 0; i < catalogue.length(); i++) {
                JSONObject item = catalogue.getJSONObject(i);

                stmt.setString(1, item.getString("itemId"));
                stmt.setString(2, item.optString("description", ""));
                stmt.setDouble(3, item.optDouble("packageCost", 0));
                stmt.setInt(4, item.optInt("availability", 0));
                stmt.executeUpdate();
                synced++;
            }

            System.out.println("[SASync] Synced " + synced + " catalogue items from SA.");
        } catch (Exception e) {
            System.err.println("[SASync] Error syncing catalogue: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==================== STEP 2: MERCHANT STATUS SYNC ====================

    /**
     * Iterates over CA's known merchants and verifies each one is still
     * marked as "active" in SA. If SA reports a merchant is no longer active,
     * CA flags them locally so that new orders are blocked until resolved.
     */
    private static void syncMerchantStatuses() {
        System.out.println("[SASync] Verifying merchant account statuses with SA...");

        String selectSql = "SELECT merchant_id FROM merchants WHERE sa_linked = 1";
        String updateSql = "UPDATE merchants SET sa_status_verified = ?, last_sa_check = CURRENT_TIMESTAMP() WHERE merchant_id = ?";

        int checked = 0;
        int flagged = 0;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement selectStmt = conn.prepareStatement(selectSql);
             ResultSet rs = selectStmt.executeQuery();
             PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {

            while (rs.next()) {
                String merchantId = rs.getString("merchant_id");
                boolean isActive = SAApiClient.checkAccountStatus(merchantId, "active");

                updateStmt.setBoolean(1, isActive);
                updateStmt.setString(2, merchantId);
                updateStmt.executeUpdate();
                checked++;

                if (!isActive) {
                    flagged++;
                    System.out.println("[SASync] Merchant " + merchantId + " is NOT active in SA.");
                }
            }

            System.out.println("[SASync] Checked " + checked + " merchants, " + flagged + " flagged as inactive.");
        } catch (Exception e) {
            System.err.println("[SASync] Error syncing merchant statuses: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==================== STEP 3: FINANCIAL DATA SYNC ====================

    /**
     * Refreshes cached discount rates, credit limits, and account balances
     * for all SA-linked merchants. This allows CA to apply correct pricing
     * and enforce credit checks without real-time SA calls on every order.
     */
    private static void syncMerchantFinancials() {
        System.out.println("[SASync] Refreshing merchant financial data from SA...");

        String selectSql = "SELECT merchant_id FROM merchants WHERE sa_linked = 1";
        String updateSql =
                "UPDATE merchants SET " +
                        "  sa_discount_rate = ?, " +
                        "  sa_credit_limit = ?, " +
                        "  sa_balance = ?, " +
                        "  last_sa_financial_sync = CURRENT_TIMESTAMP() " +
                        "WHERE merchant_id = ?";

        int updated = 0;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement selectStmt = conn.prepareStatement(selectSql);
             ResultSet rs = selectStmt.executeQuery();
             PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {

            while (rs.next()) {
                String merchantId = rs.getString("merchant_id");

                double discountRate = SAApiClient.getDiscountRate(merchantId);
                double creditLimit = SAApiClient.getCreditLimit(merchantId);
                double balance = SAApiClient.getBalance(merchantId);

                // Only update if we got valid responses (balance returns -1 on failure)
                if (balance < 0) {
                    System.out.println("[SASync] Skipping financials for merchant " + merchantId + " (SA unreachable).");
                    continue;
                }

                updateStmt.setDouble(1, discountRate);
                updateStmt.setDouble(2, creditLimit);
                updateStmt.setDouble(3, balance);
                updateStmt.setString(4, merchantId);
                updateStmt.executeUpdate();
                updated++;
            }

            System.out.println("[SASync] Updated financial data for " + updated + " merchants.");
        } catch (Exception e) {
            System.err.println("[SASync] Error syncing merchant financials: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==================== ON-DEMAND HELPERS ====================

    /**
     * Deducts stock in SA after a CA sale that involves SA-sourced items.
     * Call this when CA processes an order containing items from SA's catalogue.
     *
     * @param itemId   the SA item ID
     * @param quantity packs sold
     * @return true if SA confirmed the deduction
     */
    public static boolean deductSAStock(String itemId, int quantity) {
        System.out.println("[SASync] Deducting " + quantity + " of item " + itemId + " from SA stock...");
        boolean success = SAApiClient.deductStock(itemId, quantity);
        if (success) {
            System.out.println("[SASync] SA stock deduction confirmed.");
        } else {
            System.err.println("[SASync] SA stock deduction FAILED for item " + itemId);
        }
        return success;
    }

    /**
     * Places an order through SA on behalf of a merchant.
     *
     * @param merchantId   the merchant ID
     * @param orderDetails order details string
     * @return the SA order ID, or null on failure
     */
    public static String placeOrderViaSA(String merchantId, String orderDetails) {
        System.out.println("[SASync] Placing order via SA for merchant " + merchantId + "...");
        String orderId = SAApiClient.placeOrder(merchantId, orderDetails);
        if (orderId != null) {
            System.out.println("[SASync] SA order placed: " + orderId);
        } else {
            System.err.println("[SASync] Failed to place SA order for merchant " + merchantId);
        }
        return orderId;
    }
}