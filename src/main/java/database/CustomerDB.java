package database;

import domain.Customer;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CustomerDB {

    public static List<Customer> getAllActiveCustomers() {
        List<Customer> list = new ArrayList<>();
        String sql = "SELECT * FROM customer_accounts WHERE account_status != 'DELETED' ORDER BY full_name";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public static Customer getByAccountNumber(String accountNumber) {
        String sql = "SELECT * FROM customer_accounts WHERE account_number = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, accountNumber);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean addCustomer(Customer c) {
        String sql = """
            INSERT INTO customer_accounts
                (account_number, full_name, email, phone, address, credit_limit, discount_plan_id)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, c.getAccountNumber());
            stmt.setString(2, c.getFullName());
            stmt.setString(3, c.getEmail());
            stmt.setString(4, c.getPhone());
            stmt.setString(5, c.getAddress());
            stmt.setDouble(6, c.getCreditLimit());
            if (c.getDiscountPlanId() > 0) {
                stmt.setInt(7, c.getDiscountPlanId());
            } else {
                stmt.setNull(7, Types.INTEGER);
            }

            stmt.executeUpdate();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean updateBalance(int customerId, double newBalance) {
        String sql = "UPDATE customer_accounts SET current_balance = ? WHERE customer_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDouble(1, newBalance);
            stmt.setInt(2, customerId);
            stmt.executeUpdate();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean updateStatus(int customerId, String status) {
        String sql = "UPDATE customer_accounts SET account_status = ? WHERE customer_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status);
            stmt.setInt(2, customerId);
            stmt.executeUpdate();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean recordAccountPayment(int customerId, int invoiceId,
                                               int recordedByUserId, String paymentMethod,
                                               double amount, String notes) {
        String sql = """
            INSERT INTO account_payments (customer_id, invoice_id, recorded_by_user_id,
                payment_method, amount, notes)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, customerId);
            if (invoiceId > 0) {
                stmt.setInt(2, invoiceId);
            } else {
                stmt.setNull(2, Types.INTEGER);
            }
            stmt.setInt(3, recordedByUserId);
            stmt.setString(4, paymentMethod);
            stmt.setDouble(5, amount);
            stmt.setString(6, notes);
            stmt.executeUpdate();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static Customer getById(int customerId) {
        String sql = "SELECT * FROM customer_accounts WHERE customer_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, customerId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean updateCustomer(Customer c) {
        String sql = """
            UPDATE customer_accounts
            SET full_name = ?, email = ?, phone = ?, address = ?,
                credit_limit = ?, discount_plan_id = ?
            WHERE customer_id = ?
            """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, c.getFullName());
            stmt.setString(2, c.getEmail());
            stmt.setString(3, c.getPhone());
            stmt.setString(4, c.getAddress());
            stmt.setDouble(5, c.getCreditLimit());
            if (c.getDiscountPlanId() > 0) {
                stmt.setInt(6, c.getDiscountPlanId());
            } else {
                stmt.setNull(6, Types.INTEGER);
            }
            stmt.setInt(7, c.getCustomerId());
            stmt.executeUpdate();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static int getActiveCustomerCount() {
        String sql = "SELECT COUNT(*) FROM customer_accounts WHERE account_status = 'ACTIVE'";

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

    private static Customer mapRow(ResultSet rs) throws SQLException {
        return new Customer(
                rs.getInt("customer_id"),
                rs.getString("account_number"),
                rs.getString("full_name"),
                rs.getString("email"),
                rs.getString("phone"),
                rs.getString("address"),
                rs.getDouble("credit_limit"),
                rs.getDouble("current_balance"),
                rs.getString("account_status"),
                rs.getInt("discount_plan_id")
        );
    }

    public static boolean deleteCustomer(int customerId) {
        // Check for linked records before attempting delete
        String checkSql = """
        SELECT 
            (SELECT COUNT(*) FROM sales WHERE customer_id = ?) +
            (SELECT COUNT(*) FROM invoices WHERE customer_id = ?) +
            (SELECT COUNT(*) FROM account_payments WHERE customer_id = ?) AS total
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement check = conn.prepareStatement(checkSql)) {

            check.setInt(1, customerId);
            check.setInt(2, customerId);
            check.setInt(3, customerId);
            ResultSet rs = check.executeQuery();

            if (rs.next() && rs.getInt("total") > 0) {
                // Has linked records — soft delete instead
                String softSql = "UPDATE customer_accounts SET account_status = 'DELETED' WHERE customer_id = ?";
                try (PreparedStatement soft = conn.prepareStatement(softSql)) {
                    soft.setInt(1, customerId);
                    soft.executeUpdate();
                    return true;
                }
            }

            // No linked records — safe to hard delete
            String deleteSql = "DELETE FROM customer_accounts WHERE customer_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(deleteSql)) {
                stmt.setInt(1, customerId);
                stmt.executeUpdate();
                return true;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}