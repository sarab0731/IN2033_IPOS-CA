package database;

import domain.Product;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductDB {

    public static List<Product> getAllProducts() {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT * FROM products WHERE is_active = 1 ORDER BY item_id";

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

    public static List<Product> getLowStockProducts() {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT * FROM products WHERE is_active = 1 AND stock_quantity <= min_stock_level";

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

    public static boolean addProduct(Product p) {
        String sql = """
            INSERT INTO products (item_id, description, package_type, units_in_pack,
                                  price, vat_rate, stock_quantity, min_stock_level)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, p.getItemId());
            stmt.setString(2, p.getDescription());
            stmt.setString(3, p.getPackageType());
            stmt.setInt(4, p.getUnitsInPack());
            stmt.setDouble(5, p.getPrice());
            stmt.setDouble(6, p.getVatRate());
            stmt.setInt(7, p.getStockQuantity());
            stmt.setInt(8, p.getMinStockLevel());
            stmt.executeUpdate();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean updateProduct(Product p) {
        String sql = """
            UPDATE products SET description = ?, package_type = ?, units_in_pack = ?,
                price = ?, vat_rate = ?, stock_quantity = ?, min_stock_level = ?
            WHERE product_id = ?
            """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, p.getDescription());
            stmt.setString(2, p.getPackageType());
            stmt.setInt(3, p.getUnitsInPack());
            stmt.setDouble(4, p.getPrice());
            stmt.setDouble(5, p.getVatRate());
            stmt.setInt(6, p.getStockQuantity());
            stmt.setInt(7, p.getMinStockLevel());
            stmt.setInt(8, p.getProductId());
            stmt.executeUpdate();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean deleteProduct(int productId) {
        String sql = "UPDATE products SET is_active = 0 WHERE product_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, productId);
            stmt.executeUpdate();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean updateStock(int productId, int quantityToAdd) {
        String sql = "UPDATE products SET stock_quantity = stock_quantity + ? WHERE product_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, quantityToAdd);
            stmt.setInt(2, productId);
            stmt.executeUpdate();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static Product getByItemId(String itemId) {
        String sql = "SELECT * FROM products WHERE item_id = ? AND is_active = 1";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, itemId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Product getById(int productId) {
        String sql = "SELECT * FROM products WHERE product_id = ? AND is_active = 1";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, productId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<Product> searchProducts(String keyword) {
        List<Product> list = new ArrayList<>();
        String sql = """
            SELECT * FROM products
            WHERE is_active = 1
              AND (LOWER(description) LIKE ? OR LOWER(item_id) LIKE ?)
            ORDER BY item_id
            """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String pattern = "%" + keyword.toLowerCase() + "%";
            stmt.setString(1, pattern);
            stmt.setString(2, pattern);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public static int getActiveProductCount() {
        String sql = "SELECT COUNT(*) FROM products WHERE is_active = 1";

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

    public static int getLowStockCount() {
        String sql = """
        SELECT COUNT(*)
        FROM products
        WHERE is_active = 1 AND stock_quantity <= min_stock_level
        """;

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

    private static Product mapRow(ResultSet rs) throws SQLException {
        return new Product(
                rs.getInt("product_id"),
                rs.getString("item_id"),
                rs.getString("description"),
                rs.getString("package_type"),
                rs.getInt("units_in_pack"),
                rs.getDouble("price"),
                rs.getDouble("vat_rate"),
                rs.getInt("stock_quantity"),
                rs.getInt("min_stock_level")
        );
    }


}