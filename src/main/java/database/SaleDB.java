package database;

import domain.Customer;
import domain.Product;

import java.sql.*;
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

            // calculate totals
            double subtotal  = 0;
            double vatAmount = 0;

            for (Map.Entry<Product, Integer> entry : items.entrySet()) {
                Product p   = entry.getKey();
                int     qty = entry.getValue();
                double lineSubtotal = p.getPrice() * qty;
                double lineVat      = lineSubtotal * (p.getVatRate() / 100);
                subtotal  += lineSubtotal;
                vatAmount += lineVat;
            }

            double totalAmount = subtotal - discountAmount + vatAmount;

            // insert sale
            PreparedStatement saleStmt = conn.prepareStatement(saleSql, Statement.RETURN_GENERATED_KEYS);
            if (customer != null)
                saleStmt.setInt(1, customer.getCustomerId());
            else
                saleStmt.setNull(1, Types.INTEGER);

            saleStmt.setInt(2, processedByUserId);
            saleStmt.setString(3, saleType);
            saleStmt.setString(4, paymentMethod);
            saleStmt.setDouble(5, subtotal);
            saleStmt.setDouble(6, discountAmount);
            saleStmt.setDouble(7, vatAmount);
            saleStmt.setDouble(8, totalAmount);
            saleStmt.executeUpdate();

            ResultSet keys = saleStmt.getGeneratedKeys();
            if (!keys.next()) { conn.rollback(); return -1; }
            int saleId = keys.getInt(1);

            // insert sale items + deduct stock
            PreparedStatement itemStmt  = conn.prepareStatement(itemSql);
            PreparedStatement stockStmt = conn.prepareStatement(stockSql);

            for (Map.Entry<Product, Integer> entry : items.entrySet()) {
                Product p   = entry.getKey();
                int     qty = entry.getValue();
                double lineSubtotal = p.getPrice() * qty;
                double lineVat      = lineSubtotal * (p.getVatRate() / 100);
                double lineTotal    = lineSubtotal + lineVat;

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
            INSERT INTO invoices (invoice_number, customer_id, sale_id, amount_due, status)
            VALUES (?, ?, ?, ?, 'UNPAID')
            """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, num);
            stmt.setInt(2, customerId);
            stmt.setInt(3, saleId);
            stmt.setDouble(4, amountDue);
            stmt.executeUpdate();

            // update customer balance
            CustomerDB.updateBalance(customerId,
                    CustomerDB.getAllActiveCustomers().stream()
                            .filter(c -> c.getCustomerId() == customerId)
                            .findFirst()
                            .map(c -> c.getCurrentBalance() + amountDue)
                            .orElse(amountDue));

        } catch (Exception e) {
            e.printStackTrace();
        }
        return num;
    }
}