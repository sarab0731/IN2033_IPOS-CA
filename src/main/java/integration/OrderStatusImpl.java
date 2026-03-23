package integration;

import database.DatabaseManager;
import integration.interfaces.IOrderStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class OrderStatusImpl implements IOrderStatus {

    /**
     * SA/PU staff can check the current status of a restock order placed via CA.
     * Status values: ACCEPTED, PROCESSED, DISPATCHED, DELIVERED
     */
    @Override
    public String getOrderStatus(String orderID) {
        String sql = "SELECT status FROM restock_orders WHERE order_number = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, orderID);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) return rs.getString("status");
            return "ERROR: Order not found - " + orderID;

        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR: " + e.getMessage();
        }
    }
}