package database;

import domain.DiscountPlan;
import domain.DiscountTier;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DiscountPlanDB {

    public static List<DiscountPlan> getAllPlans() {
        List<DiscountPlan> list = new ArrayList<>();
        String sql = "SELECT * FROM discount_plans ORDER BY plan_name";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(new DiscountPlan(
                        rs.getInt("discount_plan_id"),
                        rs.getString("plan_name"),
                        rs.getString("plan_type"),
                        rs.getDouble("discount_percent"),
                        rs.getString("notes")
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public static boolean addPlan(DiscountPlan p) {
        String sql = "INSERT INTO discount_plans (plan_name, plan_type, discount_percent, notes) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, p.getPlanName());
            stmt.setString(2, p.getPlanType());
            stmt.setDouble(3, p.getDiscountPercent());
            stmt.setString(4, p.getNotes());
            stmt.executeUpdate();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static DiscountPlan getById(int planId) {
        String sql = "SELECT * FROM discount_plans WHERE discount_plan_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, planId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new DiscountPlan(
                        rs.getInt("discount_plan_id"),
                        rs.getString("plan_name"),
                        rs.getString("plan_type"),
                        rs.getDouble("discount_percent"),
                        rs.getString("notes")
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean updatePlan(DiscountPlan p) {
        String sql = """
            UPDATE discount_plans
            SET plan_name = ?, plan_type = ?, discount_percent = ?, notes = ?
            WHERE discount_plan_id = ?
            """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, p.getPlanName());
            stmt.setString(2, p.getPlanType());
            stmt.setDouble(3, p.getDiscountPercent());
            stmt.setString(4, p.getNotes());
            stmt.setInt(5, p.getDiscountPlanId());
            stmt.executeUpdate();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean deletePlan(int planId) {
        String sql = "DELETE FROM discount_plans WHERE discount_plan_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, planId);
            stmt.executeUpdate();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Tier CRUD (FLEXIBLE plans only)
    // -------------------------------------------------------------------------

    public static List<DiscountTier> getTiers(int planId) {
        List<DiscountTier> list = new ArrayList<>();
        String sql = "SELECT * FROM discount_plan_tiers WHERE discount_plan_id = ? ORDER BY min_spend";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, planId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                double maxRaw = rs.getDouble("max_spend");
                Double maxSpend = rs.wasNull() ? null : maxRaw;
                list.add(new DiscountTier(
                        rs.getInt("tier_id"), planId,
                        rs.getDouble("min_spend"), maxSpend,
                        rs.getDouble("rate")));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    public static boolean addTier(int planId, double minSpend, Double maxSpend, double rate) {
        String sql = "INSERT INTO discount_plan_tiers (discount_plan_id, min_spend, max_spend, rate) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, planId);
            stmt.setDouble(2, minSpend);
            if (maxSpend != null) stmt.setDouble(3, maxSpend); else stmt.setNull(3, Types.DECIMAL);
            stmt.setDouble(4, rate);
            stmt.executeUpdate();
            return true;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    public static boolean deleteTier(int tierId) {
        String sql = "DELETE FROM discount_plan_tiers WHERE tier_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, tierId);
            stmt.executeUpdate();
            return true;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    // -------------------------------------------------------------------------
    // Flexible discount: rate calculation
    // -------------------------------------------------------------------------

    /**
     * Given a flexible plan and a monthly spend, returns the applicable discount rate (0–100).
     * Picks the highest tier whose minSpend <= spend.
     */
    public static double resolveFlexibleRate(int planId, double monthlySpend) {
        List<DiscountTier> tiers = getTiers(planId);
        double rate = 0;
        for (DiscountTier t : tiers) {
            if (monthlySpend >= t.getMinSpend()) {
                rate = t.getRate();
            }
        }
        return rate;
    }

    // -------------------------------------------------------------------------
    // Month-end credit processing
    // -------------------------------------------------------------------------

    /**
     * Calculates and stores flexible discount credits for all customers on a
     * FLEXIBLE plan for the given calendar month.
     * Safe to run multiple times — skips customers already processed for that month.
     *
     * @param year  e.g. 2026
     * @param month 1–12
     * @return list of newly created credits: each Object[] is
     *         [creditId, customerId, fullName, "YYYY-MM", spend, rate, creditAmount]
     */
    public static List<Object[]> processMonthEndCredits(int year, int month) {
        String spendSql = """
            SELECT ca.customer_id, ca.full_name, ca.discount_plan_id,
                   COALESCE(SUM(s.subtotal), 0) AS monthly_spend
            FROM customer_accounts ca
            JOIN discount_plans dp ON ca.discount_plan_id = dp.discount_plan_id
            LEFT JOIN sales s
                ON s.customer_id = ca.customer_id
                AND s.sale_type = 'ACCOUNT'
                AND YEAR(s.sale_datetime) = ?
                AND MONTH(s.sale_datetime) = ?
            WHERE dp.plan_type = 'FLEXIBLE'
              AND ca.is_active = 1
            GROUP BY ca.customer_id, ca.full_name, ca.discount_plan_id
            """;

        String alreadyDoneSql = """
            SELECT COUNT(*) FROM flexible_discount_credits
            WHERE customer_id = ? AND period_year = ? AND period_month = ?
            """;

        String insertSql = """
            INSERT INTO flexible_discount_credits
                (customer_id, discount_plan_id, period_year, period_month,
                 monthly_spend, rate_applied, credit_amount, remaining_credit)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

        String period = year + "-" + String.format("%02d", month);
        List<Object[]> created = new ArrayList<>();

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement spendStmt = conn.prepareStatement(spendSql)) {

            spendStmt.setInt(1, year);
            spendStmt.setInt(2, month);
            ResultSet rs = spendStmt.executeQuery();

            while (rs.next()) {
                int    customerId = rs.getInt("customer_id");
                String fullName   = rs.getString("full_name");
                int    planId     = rs.getInt("discount_plan_id");
                double spend      = rs.getDouble("monthly_spend");

                // Skip if already processed for this period
                try (PreparedStatement chk = conn.prepareStatement(alreadyDoneSql)) {
                    chk.setInt(1, customerId); chk.setInt(2, year); chk.setInt(3, month);
                    ResultSet cr = chk.executeQuery();
                    if (cr.next() && cr.getInt(1) > 0) continue;
                }

                double rate   = resolveFlexibleRate(planId, spend);
                double credit = spend * (rate / 100.0);
                if (credit <= 0) continue;

                try (PreparedStatement ins = conn.prepareStatement(
                        insertSql, Statement.RETURN_GENERATED_KEYS)) {
                    ins.setInt(1, customerId);
                    ins.setInt(2, planId);
                    ins.setInt(3, year);
                    ins.setInt(4, month);
                    ins.setDouble(5, spend);
                    ins.setDouble(6, rate);
                    ins.setDouble(7, credit);
                    ins.setDouble(8, credit);  // remaining = full amount initially
                    ins.executeUpdate();

                    ResultSet keys = ins.getGeneratedKeys();
                    int creditId = keys.next() ? keys.getInt(1) : -1;
                    created.add(new Object[]{creditId, customerId, fullName, period, spend, rate, credit});
                }
                System.out.println("[DiscountPlanDB] Created credit: customer=" + customerId
                        + " (" + fullName + ") spend=£" + String.format("%.2f", spend)
                        + " rate=" + rate + "% credit=£" + String.format("%.2f", credit));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return created;
    }

    /**
     * Automatically processes flexible discount credits for every complete calendar
     * month that has FLEXIBLE-plan sales but no credit record yet, up to (but not
     * including) the current simulated month.
     *
     * Call this before reading pending credits (e.g. at sale time) so credits are
     * always available without requiring manual intervention.
     *
     * @param today the current simulated date from TimeManager
     */
    public static void autoProcessPastMonths(java.time.LocalDate today) {
        // Find the earliest sale month across all FLEXIBLE-plan customers
        String earliestSql = """
            SELECT MIN(YEAR(s.sale_datetime)), MIN(MONTH(s.sale_datetime))
            FROM sales s
            JOIN customer_accounts ca ON s.customer_id = ca.customer_id
            JOIN discount_plans dp    ON ca.discount_plan_id = dp.discount_plan_id
            WHERE dp.plan_type = 'FLEXIBLE'
              AND s.sale_type  = 'ACCOUNT'
            """;
        java.time.LocalDate firstMonth = null;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(earliestSql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next() && rs.getObject(1) != null) {
                firstMonth = java.time.LocalDate.of(rs.getInt(1), rs.getInt(2), 1);
            }
        } catch (Exception e) { e.printStackTrace(); }

        if (firstMonth == null) return; // no FLEXIBLE sales at all

        // Process every month from firstMonth up to last complete month
        java.time.LocalDate cursor   = firstMonth.withDayOfMonth(1);
        java.time.LocalDate lastDone = today.withDayOfMonth(1); // exclusive upper bound

        while (cursor.isBefore(lastDone)) {
            processMonthEndCredits(cursor.getYear(), cursor.getMonthValue());
            cursor = cursor.plusMonths(1);
        }
    }

    /**
     * Returns true if any FLEXIBLE-plan customer had ACCOUNT sales in the given
     * month but has not yet been processed (no credit record for that period).
     * Used to auto-detect when the panel opens whether month-end is overdue.
     */
    public static boolean hasUnprocessedFlexibleMonth(int year, int month) {
        String sql = """
            SELECT COUNT(DISTINCT ca.customer_id)
            FROM customer_accounts ca
            JOIN discount_plans dp ON ca.discount_plan_id = dp.discount_plan_id
            JOIN sales s ON s.customer_id = ca.customer_id
                AND s.sale_type = 'ACCOUNT'
                AND YEAR(s.sale_datetime) = ?
                AND MONTH(s.sale_datetime) = ?
            WHERE dp.plan_type = 'FLEXIBLE'
              AND ca.is_active = 1
              AND NOT EXISTS (
                SELECT 1 FROM flexible_discount_credits fdc
                WHERE fdc.customer_id = ca.customer_id
                  AND fdc.period_year = ?
                  AND fdc.period_month = ?
              )
            """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, year); stmt.setInt(2, month);
            stmt.setInt(3, year); stmt.setInt(4, month);
            ResultSet rs = stmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    // -------------------------------------------------------------------------
    // Flexible discount: credit application at next sale
    // -------------------------------------------------------------------------

    /**
     * Returns the total pending (remaining) credit for a customer across all months.
     */
    public static double getPendingCredit(int customerId) {
        String sql = """
            SELECT COALESCE(SUM(remaining_credit), 0)
            FROM flexible_discount_credits
            WHERE customer_id = ? AND remaining_credit > 0
            """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, customerId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getDouble(1);
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    /**
     * Consumes up to {@code amount} from the customer's oldest pending credits (FIFO).
     * Returns the actual amount consumed (may be less if credit runs out).
     */
    public static double consumeCredit(int customerId, double amount) {
        String fetchSql = """
            SELECT credit_id, remaining_credit FROM flexible_discount_credits
            WHERE customer_id = ? AND remaining_credit > 0
            ORDER BY period_year, period_month, credit_id
            """;
        String updateSql = "UPDATE flexible_discount_credits SET remaining_credit = ? WHERE credit_id = ?";

        double consumed = 0;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement fetch = conn.prepareStatement(fetchSql);
             PreparedStatement update = conn.prepareStatement(updateSql)) {

            fetch.setInt(1, customerId);
            ResultSet rs = fetch.executeQuery();
            double remaining = amount;

            while (rs.next() && remaining > 0) {
                int    creditId    = rs.getInt("credit_id");
                double avail       = rs.getDouble("remaining_credit");
                double take        = Math.min(avail, remaining);
                double newRemaining = avail - take;

                update.setDouble(1, newRemaining);
                update.setInt(2, creditId);
                update.executeUpdate();

                consumed  += take;
                remaining -= take;
            }
        } catch (Exception e) { e.printStackTrace(); }
        return consumed;
    }

    /**
     * Returns all credits for display in DiscountPlansPanel.
     * Each row: [creditId, customerId, fullName, period, monthlySpend, rate,
     *            creditAmount, status, remaining]
     * status = "Pending" | "Partially Used" | "Consumed"
     */
    public static List<Object[]> getAllCredits() {
        List<Object[]> list = new ArrayList<>();
        String sql = """
            SELECT fdc.credit_id, ca.customer_id, ca.full_name,
                   fdc.period_year, fdc.period_month,
                   fdc.monthly_spend, fdc.rate_applied,
                   fdc.credit_amount, fdc.remaining_credit
            FROM flexible_discount_credits fdc
            JOIN customer_accounts ca ON fdc.customer_id = ca.customer_id
            ORDER BY fdc.period_year DESC, fdc.period_month DESC, ca.full_name
            """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                double remaining = rs.getDouble("remaining_credit");
                double creditAmt = rs.getDouble("credit_amount");
                String status;
                if (remaining <= 0) {
                    status = "Consumed";
                } else if (remaining < creditAmt) {
                    status = "Partially Used";
                } else {
                    status = "Pending";
                }
                list.add(new Object[]{
                        rs.getInt("credit_id"),
                        rs.getInt("customer_id"),
                        rs.getString("full_name"),
                        rs.getString("period_year") + "-" + String.format("%02d", rs.getInt("period_month")),
                        String.format("£%.2f", rs.getDouble("monthly_spend")),
                        String.format("%.1f%%", rs.getDouble("rate_applied")),
                        String.format("£%.2f", creditAmt),
                        status,
                        String.format("£%.2f", remaining)
                });
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }
}