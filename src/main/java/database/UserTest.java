package database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UserTest {

    public static void insertUser(String username, String passwordHash, String fullName, String role) {

        String sql = """
                INSERT INTO users (username, password_hash, full_name, role)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, passwordHash);
            stmt.setString(3, fullName);
            stmt.setString(4, role);

            stmt.executeUpdate();

            System.out.println("User inserted: " + username);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void printUsers() {

        String sql = "SELECT * FROM users";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            System.out.println("---- USERS TABLE ----");

            while (rs.next()) {

                int id = rs.getInt("user_id");
                String username = rs.getString("username");
                String fullName = rs.getString("full_name");
                String role = rs.getString("role");

                System.out.println(
                        id + " | " +
                                username + " | " +
                                fullName + " | " +
                                role
                );
            }

            System.out.println("---------------------");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void printProducts() {
        String sql = "SELECT * FROM products WHERE is_active = 1";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            System.out.println("---- PRODUCTS TABLE ----");
            while (rs.next()) {
                System.out.println(
                        rs.getInt("product_id") + " | " +
                                rs.getString("item_id") + " | " +
                                rs.getString("description") + " | " +
                                rs.getString("package_type") + " | " +
                                rs.getDouble("price") + " | " +
                                rs.getInt("stock_quantity") + " / " +
                                rs.getInt("min_stock_level")
                );
            }
            System.out.println("------------------------");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void printCustomers() {
        String sql = "SELECT * FROM customer_accounts";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            System.out.println("---- CUSTOMER ACCOUNTS ----");
            while (rs.next()) {
                System.out.println(
                        rs.getInt("customer_id") + " | " +
                                rs.getString("account_number") + " | " +
                                rs.getString("full_name") + " | " +
                                rs.getString("account_status") + " | " +
                                "Balance: " + rs.getDouble("current_balance") + " | " +
                                "Limit: " + rs.getDouble("credit_limit")
                );
            }
            System.out.println("---------------------------");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void printSales() {
        String sql = "SELECT * FROM sales ORDER BY sale_datetime DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            System.out.println("---- SALES TABLE ----");
            while (rs.next()) {
                System.out.println(
                        rs.getInt("sale_id") + " | " +
                                rs.getString("sale_type") + " | " +
                                rs.getString("payment_method") + " | " +
                                "Total: " + rs.getDouble("total_amount") + " | " +
                                rs.getString("sale_datetime")
                );
            }
            System.out.println("---------------------");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void printRestockOrders() {
        String sql = "SELECT * FROM restock_orders ORDER BY created_at DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            System.out.println("---- RESTOCK ORDERS ----");
            while (rs.next()) {
                System.out.println(
                        rs.getInt("restock_order_id") + " | " +
                                rs.getString("order_number") + " | " +
                                rs.getString("merchant_id") + " | " +
                                rs.getString("status") + " | " +
                                "Value: " + rs.getDouble("total_value")
                );
            }
            System.out.println("------------------------");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void printInvoices() {
        String sql = "SELECT * FROM invoices ORDER BY invoice_date DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            System.out.println("---- INVOICES TABLE ----");
            while (rs.next()) {
                System.out.println(
                        rs.getInt("invoice_id") + " | " +
                                rs.getString("invoice_number") + " | " +
                                "Customer: " + rs.getInt("customer_id") + " | " +
                                "Due: " + rs.getDouble("amount_due") + " | " +
                                rs.getString("status")
                );
            }
            System.out.println("------------------------");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void printDiscountPlans() {
        String sql = "SELECT * FROM discount_plans";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            System.out.println("---- DISCOUNT PLANS ----");
            while (rs.next()) {
                System.out.println(
                        rs.getInt("discount_plan_id") + " | " +
                                rs.getString("plan_name") + " | " +
                                rs.getString("plan_type") + " | " +
                                rs.getDouble("discount_percent") + "%"
                );
            }
            System.out.println("------------------------");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}