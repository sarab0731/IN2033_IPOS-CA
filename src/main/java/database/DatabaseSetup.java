package database;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import integration.PUSync;
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
                    stmt.executeUpdate(trimmed);
                }
            }

            conn.commit();
            System.out.println("Database initialised.");

            seedDefaultUsers(conn);

            // Sync with PU system (pull pending stock changes, push product catalog)
            PUSync.syncWithPU();

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