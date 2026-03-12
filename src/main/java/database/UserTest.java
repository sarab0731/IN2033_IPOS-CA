package database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UserTest {

    //Here you should create different methods to insert other elements.

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

    //here you can make new method to print other tables.
}