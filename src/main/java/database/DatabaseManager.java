package database;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseManager {

    private static final String URL;
    private static final String USER;
    private static final String PASSWORD;

    static {
        // Force the MySQL driver to register itself (required when running outside a fat JAR)
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("[DatabaseManager] MySQL driver not found on classpath: " + e.getMessage());
        }

        Properties props = new Properties();

        // 1. Try db.properties next to the JAR / in the working directory
        File externalFile = new File("db.properties");
        if (externalFile.exists()) {
            try (InputStream in = new FileInputStream(externalFile)) {
                props.load(in);
            } catch (Exception e) {
                System.err.println("[DatabaseManager] Could not read db.properties: " + e.getMessage());
            }
        } else {
            // 2. Fall back to the bundled resource inside the JAR / classpath
            try (InputStream in = DatabaseManager.class.getClassLoader()
                    .getResourceAsStream("db.properties")) {
                if (in != null) props.load(in);
            } catch (Exception e) {
                System.err.println("[DatabaseManager] Could not load bundled db.properties: " + e.getMessage());
            }
        }

        // 3. Final fallback: hardcoded defaults so the app never refuses to start
        URL      = props.getProperty("db.url",      "jdbc:mysql://localhost:3306/IPOS-CA");
        USER     = props.getProperty("db.user",     "root");
        PASSWORD = props.getProperty("db.password", "root");
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}