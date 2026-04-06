package database;

import domain.Customer;
import domain.Product;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SaleDB {

    /**
     * Records a full sale. items is a map of Product -> quantity.
     * Returns the generated sale_id or -1 on failure.
     */
    public static int recordSale(int processedByUserId, Customer customer,
                                 String saleType, String paymentMethod,
                                 Map<Product, Integer> items, double discountAmount) {

        String saleSql = """
            INSERT INTO sales (customer_id, processed_by_user_id, sale_type,
                payment_method, subtotal, discount_amount, vat_amount, total_amount)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

        String itemSql = """
            INSERT INTO sale_items (sale_id, product_id, quantity, unit_price,
                vat_rate, discount_amount, line_total)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

        String stockSql = "UPDATE products SET stock_quantity = stock_quantity - ? WHERE product_id = ?";

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);

            double subtotal = 0;
            double vatAmount = 0;

            for (Map.Entry<Product, Integer> entry : items.entrySet()) {
                Product p = entry.getKey();
                int qty = entry.getValue();
                double lineSubtotal = p.getPrice() * qty;
                double lineVat = lineSubtotal * (p.getVatRate() / 100);
                subtotal += lineSubtotal;
                vatAmount += lineVat;
            }

            double totalAmount = subtotal - discountAmount + vatAmount;

            PreparedStatement saleStmt = conn.prepareStatement(saleSql, Statement.RETURN_GENERATED_KEYS);
            if (customer != null) {
                saleStmt.setInt(1, customer.getCustomerId());
            } else {
                saleStmt.setNull(1, Types.INTEGER);
            }

            saleStmt.setInt(2, processedByUserId);
            saleStmt.setString(3, saleType);
            saleStmt.setString(4, paymentMethod);
            saleStmt.setDouble(5, subtotal);
            saleStmt.setDouble(6, discountAmount);
            saleStmt.setDouble(7, vatAmount);
            saleStmt.setDouble(8, totalAmount);
            saleStmt.executeUpdate();

            ResultSet keys = saleStmt.getGeneratedKeys();
            if (!keys.next()) {
                conn.rollback();
                return -1;
            }
            int saleId = keys.getInt(1);

            PreparedStatement itemStmt = conn.prepareStatement(itemSql);
            PreparedStatement stockStmt = conn.prepareStatement(stockSql);

            for (Map.Entry<Product, Integer> entry : items.entrySet()) {
                Product p = entry.getKey();
                int qty = entry.getValue();
                double lineSubtotal = p.getPrice() * qty;
                double lineVat = lineSubtotal * (p.getVatRate() / 100);
                double lineTotal = lineSubtotal + lineVat;

                itemStmt.setInt(1, saleId);
                itemStmt.setInt(2, p.getProductId());
                itemStmt.setInt(3, qty);
                itemStmt.setDouble(4, p.getPrice());
                itemStmt.setDouble(5, p.getVatRate());
                itemStmt.setDouble(6, 0.00);
                itemStmt.setDouble(7, lineTotal);
                itemStmt.executeUpdate();

                stockStmt.setInt(1, qty);
                stockStmt.setInt(2, p.getProductId());
                stockStmt.executeUpdate();
            }

            conn.commit();
            return saleId;

        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    /** Generates a receipt number and inserts into receipts table */
    public static String generateReceipt(int saleId) {
        String num = "RCP-" + System.currentTimeMillis();
        String sql = "INSERT INTO receipts (receipt_number, sale_id) VALUES (?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, num);
            stmt.setInt(2, saleId);
            stmt.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return num;
    }

    /** Generates an invoice number and inserts into invoices table */
    public static String generateInvoice(int saleId, int customerId, double amountDue) {
        String num = "INV-" + System.currentTimeMillis();
        String sql = """
            INSERT INTO invoices (invoice_number, customer_id, sale_id, amount_due, due_date, status)
            VALUES (?, ?, ?, ?, DATE_ADD(CURDATE(), INTERVAL 30 DAY), 'UNPAID')
            """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, num);
            stmt.setInt(2, customerId);
            stmt.setInt(3, saleId);
            stmt.setDouble(4, amountDue);
            stmt.executeUpdate();

            CustomerDB.updateBalance(
                    customerId,
                    CustomerDB.getAllActiveCustomers().stream()
                            .filter(c -> c.getCustomerId() == customerId)
                            .findFirst()
                            .map(c -> c.getCurrentBalance() + amountDue)
                            .orElse(amountDue)
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
        return num;
    }

    /**
     * Returns total turnover (sum of total_amount) for a date range.
     */
    public static double getTurnover(String fromDate, String toDate) {
        String sql = """
            SELECT COALESCE(SUM(total_amount), 0) AS turnover
            FROM sales
            WHERE DATE(sale_datetime) BETWEEN ? AND ?
            """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, fromDate);
            stmt.setString(2, toDate);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getDouble("turnover");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Returns per-product sales breakdown for a date range.
     * Each map entry: item_id, description, qty_sold, revenue.
     */
    public static List<Map<String, Object>> getSalesByProduct(String fromDate, String toDate) {
        List<Map<String, Object>> results = new ArrayList<>();
        String sql = """
            SELECT p.item_id, p.description,
                   SUM(si.quantity) AS qty_sold,
                   SUM(si.line_total) AS revenue
            FROM sale_items si
            JOIN products p ON si.product_id = p.product_id
            JOIN sales s    ON si.sale_id     = s.sale_id
            WHERE DATE(s.sale_datetime) BETWEEN ? AND ?
            GROUP BY p.product_id
            ORDER BY revenue DESC
            """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, fromDate);
            stmt.setString(2, toDate);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("item_id", rs.getString("item_id"));
                row.put("description", rs.getString("description"));
                row.put("qty_sold", rs.getInt("qty_sold"));
                row.put("revenue", rs.getDouble("revenue"));
                results.add(row);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return results;
    }

    /**
     * Generates a monthly statement for a customer covering a date range.
     * Returns the statement number or null on failure.
     */
    public static String generateMonthlyStatement(int customerId, String periodStart,
                                                  String periodEnd, double totalDue) {
        String num = "STM-" + System.currentTimeMillis();
        String sql = """
            INSERT INTO monthly_statements
                (statement_number, customer_id, period_start, period_end, total_due, status)
            VALUES (?, ?, ?, ?, ?, 'GENERATED')
            """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, num);
            stmt.setInt(2, customerId);
            stmt.setString(3, periodStart);
            stmt.setString(4, periodEnd);
            stmt.setDouble(5, totalDue);
            stmt.executeUpdate();
            return num;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Returns all unpaid/partially paid invoices for a customer.
     */
    public static List<Map<String, Object>> getUnpaidInvoices(int customerId) {
        List<Map<String, Object>> results = new ArrayList<>();
        String sql = """
            SELECT invoice_id, invoice_number, amount_due, status, invoice_date
            FROM invoices
            WHERE customer_id = ? AND status IN ('UNPAID', 'PARTIALLY_PAID', 'OVERDUE')
            ORDER BY invoice_date
            """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, customerId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("invoice_id", rs.getInt("invoice_id"));
                row.put("invoice_number", rs.getString("invoice_number"));
                row.put("amount_due", rs.getDouble("amount_due"));
                row.put("status", rs.getString("status"));
                row.put("invoice_date", rs.getString("invoice_date"));
                results.add(row);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return results;
    }

    /**
     * Updates an invoice status (UNPAID, PARTIALLY_PAID, PAID, OVERDUE).
     */
    public static boolean updateInvoiceStatus(int invoiceId, String newStatus) {
        String sql = "UPDATE invoices SET status = ? WHERE invoice_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, newStatus);
            stmt.setInt(2, invoiceId);
            stmt.executeUpdate();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static int getSalesCount() {
        String sql = "SELECT COUNT(*) FROM sales";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static double getTotalSalesValue() {
        String sql = "SELECT COALESCE(SUM(total_amount), 0) FROM sales";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getDouble(1);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static double getSalesValueLastDays(int days) {
        String sql = """
            SELECT COALESCE(SUM(total_amount), 0)
            FROM sales
            WHERE DATE(sale_datetime) >= DATE('now', ?)
            """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, "-" + days + " day");
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getDouble(1);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
}