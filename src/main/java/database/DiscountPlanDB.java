package database;

import domain.DiscountPlan;

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
}