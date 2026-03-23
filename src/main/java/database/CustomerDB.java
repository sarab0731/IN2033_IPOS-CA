package database;

import domain.Customer;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CustomerDB {

    public static List<Customer> getAllActiveCustomers() {
        List<Customer> list = new ArrayList<>();
        String sql = "SELECT * FROM customer_accounts ORDER BY full_name";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) list.add(mapRow(rs));

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
            if (rs.next()) return mapRow(rs);

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
            if (c.getDiscountPlanId() > 0)
                stmt.setInt(7, c.getDiscountPlanId());
            else
                stmt.setNull(7, Types.INTEGER);

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
}