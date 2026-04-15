package database;

import domain.Merchant;
import domain.SACatalogueItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MerchantDB {

    // ── Query ─────────────────────────────────────────────────────────────────

    /**
     * Returns the single configured SA merchant for this CA installation,
     * or null if the merchant's table is empty (SA not yet configured).
     */
    public static Merchant getConfiguredMerchant() {
        String sql = "SELECT * FROM merchants LIMIT 1";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return mapRow(rs);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // ── Updates called by SASync ──────────────────────────────────────────────

    /**
     * Updates the SA-verified status and derived account status for a merchant.
     * Called by SASync.syncMerchantStatuses().
     */
    public static void updateStatus(String merchantId, boolean verified, String accountStatus) {
        // Normalise to uppercase — SA returns lowercase ("normal", "suspended", "in_default")
        // but CA's CHECK constraint requires uppercase ("NORMAL", "SUSPENDED", "IN_DEFAULT")
        String normalised = accountStatus != null ? accountStatus.toUpperCase() : "NORMAL";
        String sql = "UPDATE merchants SET sa_status_verified = ?, sa_account_status = ?, " +
                     "last_sa_check = CURRENT_TIMESTAMP() WHERE merchant_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBoolean(1, verified);
            stmt.setString(2, normalised);
            stmt.setString(3, merchantId);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Refreshes cached financial data for a merchant.
     * Called by SASync.syncMerchantFinancials().
     */
    public static void updateFinancials(String merchantId, double discountRate,
                                        double creditLimit, double balance) {
        String sql = "UPDATE merchants SET sa_discount_rate = ?, sa_credit_limit = ?, " +
                     "sa_balance = ?, last_sa_financial_sync = CURRENT_TIMESTAMP() " +
                     "WHERE merchant_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, discountRate);
            stmt.setDouble(2, creditLimit);
            stmt.setDouble(3, balance);
            stmt.setString(4, merchantId);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── SA Catalogue cache queries ────────────────────────────────────────────

    /**
     * Reads all items from the local SA catalogue cache.
     * SA only provides: itemId, description, packageCost, availability.
     * Category and manufacturer are not supplied by SA — left empty.
     * If the cache is empty (SA was offline at startup), returns an empty list.
     */
    public static List<SACatalogueItem> getCatalogueItems() {
        List<SACatalogueItem> items = new ArrayList<>();
        String sql = "SELECT item_id, description, package_cost, availability FROM sa_catalogue_cache ORDER BY item_id";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                items.add(new SACatalogueItem(
                        rs.getString("item_id"),
                        rs.getString("description"),
                        "",                           // SA does not send category
                        rs.getDouble("package_cost"),
                        1,                            // SA does not send pack size
                        "",                           // SA does not send manufacturer
                        rs.getInt("availability")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return items;
    }

    // ── Product resolution for SA orders ─────────────────────────────────────

    /**
     * Ensures an SA catalogue item exists in the local products table so that
     * restock_order_items can reference it via FK.
     * If the item is not yet in products, it is inserted with stock_quantity = 0.
     *
     * @return the product_id, or -1 on failure
     */
    public static int ensureProduct(SACatalogueItem item) {
        String checkSql  = "SELECT product_id FROM products WHERE item_id = ?";
        String insertSql = "INSERT INTO products " +
                           "(item_id, description, package_type, units_in_pack, price, vat_rate, stock_quantity, min_stock_level) " +
                           "VALUES (?, ?, ?, ?, ?, 0.0, 0, 0)";
        try (Connection conn = DatabaseManager.getConnection()) {
            try (PreparedStatement check = conn.prepareStatement(checkSql)) {
                check.setString(1, item.getItemId());
                ResultSet rs = check.executeQuery();
                if (rs.next()) return rs.getInt("product_id");
            }
            try (PreparedStatement ins = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                ins.setString(1, item.getItemId());
                ins.setString(2, item.getDescription());
                ins.setString(3, "Pack of " + item.getPackSize());
                ins.setInt(4, item.getPackSize());
                ins.setDouble(5, item.getUnitCost());
                ins.executeUpdate();
                ResultSet keys = ins.getGeneratedKeys();
                if (keys.next()) return keys.getInt(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static Merchant mapRow(ResultSet rs) throws SQLException {
        return new Merchant(
                rs.getString("merchant_id"),
                rs.getBoolean("sa_linked"),
                rs.getBoolean("sa_status_verified"),
                rs.getString("sa_account_status"),
                rs.getDouble("sa_discount_rate"),
                rs.getDouble("sa_credit_limit"),
                rs.getDouble("sa_balance")
        );
    }

}