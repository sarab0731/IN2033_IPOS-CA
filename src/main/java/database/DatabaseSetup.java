package database;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import org.mindrot.jbcrypt.BCrypt;

public class DatabaseSetup {

    public static void initialise() {
        try (Connection conn = DatabaseManager.getConnection()) {

            conn.setAutoCommit(false);

            Statement stmt = conn.createStatement();

            String sql = Files.readString(Paths.get("sql/schema.sql"));
            String[] statements = sql.split(";");
            for (String s : statements) {
                String trimmed = s.trim();
                if (!trimmed.isEmpty()) {
                    try {
                        stmt.executeUpdate(trimmed);
                    } catch (Exception stmtEx) {
                        System.err.println("[DatabaseSetup] Skipping statement (error: " + stmtEx.getMessage() + "): " + trimmed.substring(0, Math.min(80, trimmed.length())));
                    }
                }
            }

            conn.commit();
            System.out.println("Database initialised.");

            // Run Java-based migrations as a safety net — works on all MySQL versions
            // by using information_schema to check existence before altering.
            runMigrations();

            seedDefaultUsers(conn);
            seedDefaultMerchant(conn);

            // Note: Sync with PU is now handled in Main.java after pull completes

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Ensures all required columns exist, using information_schema lookups so the
     * check is idempotent and compatible with every MySQL 5.x / 8.x version.
     */
    private static void runMigrations() {
        addColumnIfMissing("merchants",      "company_name",        "VARCHAR(255) NOT NULL DEFAULT ''");
        addColumnIfMissing("merchants",      "registration_number", "VARCHAR(100)");
        addColumnIfMissing("merchants",      "email",               "VARCHAR(255)");
        addColumnIfMissing("merchants",      "phone",               "VARCHAR(20)");
        addColumnIfMissing("merchants",      "address",             "TEXT");
        addColumnIfMissing("merchants",      "sa_account_status",   "VARCHAR(20) NOT NULL DEFAULT 'NORMAL'");
        addColumnIfMissing("restock_orders", "sa_order_id",         "VARCHAR(100) NULL");
    }

    private static void addColumnIfMissing(String table, String column, String definition) {
        try (Connection conn = DatabaseManager.getConnection()) {
            String checkSql =
                    "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                    "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?";
            PreparedStatement check = conn.prepareStatement(checkSql);
            check.setString(1, table);
            check.setString(2, column);
            ResultSet rs = check.executeQuery();
            rs.next();
            if (rs.getInt(1) == 0) {
                conn.createStatement().executeUpdate(
                        "ALTER TABLE `" + table + "` ADD COLUMN `" + column + "` " + definition);
                System.out.println("[DatabaseSetup] Migration: added " + table + "." + column);
            }
        } catch (Exception e) {
            System.err.println("[DatabaseSetup] Migration failed for " + table + "." + column + ": " + e.getMessage());
        }
    }

    private static void seedDefaultMerchant(Connection conn) {
        try {
            database.MerchantDB.seedM001Public(conn);
            conn.commit();
            System.out.println("Merchant M001 (Cosymed Ltd) seeded/updated.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void seedDefaultUsers(Connection conn) {
        try {
            ResultSet rs = conn.createStatement()
                    .executeQuery("SELECT COUNT(*) FROM users");
            rs.next();
            if (rs.getInt(1) > 0) return;

            String sql = "INSERT INTO users (username, password_hash, full_name, user_role) VALUES (?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);

            String[][] users = {
                    {"admin",       "admin123",   "Administrator",   "ADMIN"},
                    {"manager",     "manager123", "Store Manager",   "MANAGER"},
                    {"pharmacist",  "pharma123",  "John Pharmacist", "PHARMACIST"}
            };

            for (String[] u : users) {
                stmt.setString(1, u[0]);
                stmt.setString(2, BCrypt.hashpw(u[1], BCrypt.gensalt()));
                stmt.setString(3, u[2]);
                stmt.setString(4, u[3]);
                stmt.executeUpdate();
            }

            conn.commit();
            System.out.println("Default users seeded.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
