package database;

import domain.Product;
import domain.RestockOrder;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RestockOrderDB {

    public static List<RestockOrder> getAllOrders() {
        List<RestockOrder> list = new ArrayList<>();
        String sql = "SELECT * FROM restock_orders ORDER BY created_at DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(new RestockOrder(
                        rs.getInt("restock_order_id"),
                        rs.getString("order_number"),
                        rs.getString("merchant_id"),
                        rs.getString("status"),
                        rs.getDouble("total_value"),
                        rs.getString("created_at")
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Places a new restock order.
     * items = map of Product -> quantity ordered
     * Returns the order number or null on failure.
     */
    public static String placeOrder(String merchantId, Map<Product, Integer> items) {
        String orderNumber = "RST-" + System.currentTimeMillis();

        String orderSql = """
            INSERT INTO restock_orders (order_number, merchant_id, status, total_value)
            VALUES (?, ?, 'ACCEPTED', ?)
            """;

        String itemSql = """
            INSERT INTO restock_order_items (restock_order_id, product_id, quantity, unit_cost, line_total)
            VALUES (?, ?, ?, ?, ?)
            """;

        String historySql = """
            INSERT INTO order_status_history (restock_order_id, old_status, new_status)
            VALUES (?, NULL, 'ACCEPTED')
            """;

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);

            double totalValue = items.entrySet().stream()
                    .mapToDouble(e -> e.getKey().getPrice() * e.getValue())
                    .sum();

            PreparedStatement orderStmt = conn.prepareStatement(orderSql, Statement.RETURN_GENERATED_KEYS);
            orderStmt.setString(1, orderNumber);
            orderStmt.setString(2, merchantId);
            orderStmt.setDouble(3, totalValue);
            orderStmt.executeUpdate();

            ResultSet keys = orderStmt.getGeneratedKeys();
            if (!keys.next()) { conn.rollback(); return null; }
            int orderId = keys.getInt(1);

            PreparedStatement itemStmt = conn.prepareStatement(itemSql);
            for (Map.Entry<Product, Integer> entry : items.entrySet()) {
                Product p   = entry.getKey();
                int     qty = entry.getValue();
                double  lineTotal = p.getPrice() * qty;

                itemStmt.setInt(1, orderId);
                itemStmt.setInt(2, p.getProductId());
                itemStmt.setInt(3, qty);
                itemStmt.setDouble(4, p.getPrice());
                itemStmt.setDouble(5, lineTotal);
                itemStmt.executeUpdate();
            }

            PreparedStatement histStmt = conn.prepareStatement(historySql);
            histStmt.setInt(1, orderId);
            histStmt.executeUpdate();

            conn.commit();
            return orderNumber;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Updates order status and logs it in order_status_history.
     * Valid transitions: ACCEPTED -> PROCESSED -> DISPATCHED -> DELIVERED
     * When status becomes DELIVERED, stock quantities are automatically updated.
     */
    public static boolean updateStatus(int restockOrderId, String oldStatus, String newStatus) {
        String updateSql  = "UPDATE restock_orders SET status = ? WHERE restock_order_id = ?";
        String historySql = """
            INSERT INTO order_status_history (restock_order_id, old_status, new_status)
            VALUES (?, ?, ?)
            """;

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);

            PreparedStatement updateStmt = conn.prepareStatement(updateSql);
            updateStmt.setString(1, newStatus);
            updateStmt.setInt(2, restockOrderId);
            updateStmt.executeUpdate();

            PreparedStatement histStmt = conn.prepareStatement(historySql);
            histStmt.setInt(1, restockOrderId);
            histStmt.setString(2, oldStatus);
            histStmt.setString(3, newStatus);
            histStmt.executeUpdate();

            if ("DELIVERED".equals(newStatus)) {
                receiveStock(conn, restockOrderId);
            }

            conn.commit();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * When an order is delivered, add the ordered quantities back into product stock.
     */
    private static void receiveStock(Connection conn, int restockOrderId) throws SQLException {
        String itemsSql = "SELECT product_id, quantity FROM restock_order_items WHERE restock_order_id = ?";
        String stockSql = "UPDATE products SET stock_quantity = stock_quantity + ? WHERE product_id = ?";

        PreparedStatement itemsStmt = conn.prepareStatement(itemsSql);
        itemsStmt.setInt(1, restockOrderId);
        ResultSet rs = itemsStmt.executeQuery();

        PreparedStatement stockStmt = conn.prepareStatement(stockSql);
        while (rs.next()) {
            stockStmt.setInt(1, rs.getInt("quantity"));
            stockStmt.setInt(2, rs.getInt("product_id"));
            stockStmt.executeUpdate();
        }
    }

    /**
     * Gets a single order by its order number.
     */
    public static RestockOrder getByOrderNumber(String orderNumber) {
        String sql = "SELECT * FROM restock_orders WHERE order_number = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, orderNumber);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new RestockOrder(
                        rs.getInt("restock_order_id"),
                        rs.getString("order_number"),
                        rs.getString("merchant_id"),
                        rs.getString("status"),
                        rs.getDouble("total_value"),
                        rs.getString("created_at")
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Returns orders filtered by status.
     */
    public static List<RestockOrder> getOrdersByStatus(String status) {
        List<RestockOrder> list = new ArrayList<>();
        String sql = "SELECT * FROM restock_orders WHERE status = ? ORDER BY created_at DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(new RestockOrder(
                        rs.getInt("restock_order_id"),
                        rs.getString("order_number"),
                        rs.getString("merchant_id"),
                        rs.getString("status"),
                        rs.getDouble("total_value"),
                        rs.getString("created_at")
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
}