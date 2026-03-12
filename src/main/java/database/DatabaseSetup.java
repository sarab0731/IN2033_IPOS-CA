package database;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Statement;

public class DatabaseSetup {

    public static void initialise() {

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("PRAGMA foreign_keys = ON;");

            String sql = Files.readString(Paths.get("sql/schema.sql"));
            stmt.executeUpdate(sql);

            System.out.println("Database initialised.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}