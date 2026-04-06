package database;

import domain.User;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class UserDB {

    public static User authenticate(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND is_active = 1";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String storedHash = rs.getString("password_hash");
                if (BCrypt.checkpw(password, storedHash)) {
                    return new User(
                            rs.getInt("user_id"),
                            rs.getString("username"),
                            rs.getString("full_name"),
                            rs.getString("role")
                    );
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static void insertUser(String username, String plainPassword, String fullName, String role) {
        String hash = BCrypt.hashpw(plainPassword, BCrypt.gensalt());
        String sql = "INSERT INTO users (username, password_hash, full_name, role) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, hash);
            stmt.setString(3, fullName);
            stmt.setString(4, role);
            stmt.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<User> getActiveUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE is_active = 1 ORDER BY full_name";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                users.add(new User(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("full_name"),
                        rs.getString("role")
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return users;
    }

    public static boolean usernameExists(String username) {
        String sql = "SELECT 1 FROM users WHERE username = ? AND is_active = 1";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username.trim());

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public static boolean isAdmin(User user) {
        if (user == null || user.getRole() == null) {
            return false;
        }
        String role = user.getRole().trim().toLowerCase();
        return role.equals("admin") || role.equals("administrator");
    }

    public static boolean authenticateAdmin(String username, String password) {
        User user = authenticate(username, password);
        return isAdmin(user);
    }

    public static boolean resetPasswordByUsername(String username, String newPassword) {
        if (username == null || newPassword == null) {
            return false;
        }

        username = username.trim();
        newPassword = newPassword.trim();

        if (username.isEmpty() || newPassword.isEmpty()) {
            return false;
        }

        if (!usernameExists(username)) {
            return false;
        }

        String sql = "UPDATE users SET password_hash = ? WHERE username = ? AND is_active = 1";
        String newHash = BCrypt.hashpw(newPassword, BCrypt.gensalt());

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, newHash);
            stmt.setString(2, username);

            return stmt.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }
}