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

        // check docker env vars
        String envUrl  = System.getenv("DB_URL");
        String envUser = System.getenv("DB_USER");
        String envPass = System.getenv("DB_PASSWORD");

        URL      = (envUrl  != null && !envUrl.isEmpty())  ? envUrl  : props.getProperty("db.url",      "jdbc:mysql://localhost:3306/IPOS-CA");
        USER     = (envUser != null && !envUser.isEmpty()) ? envUser : props.getProperty("db.user",     "root");
        PASSWORD = (envPass != null && !envPass.isEmpty()) ? envPass : props.getProperty("db.password", "root");

        System.out.println("[DatabaseManager] Connecting to: " + URL + " as " + USER);
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    /**
     * Applies incremental DDL migrations on every startup.
     * All operations are idempotent — safe to run on every boot.
     */
    public static void runMigrations() {
        try (Connection conn = getConnection();
             java.sql.Statement stmt = conn.createStatement()) {

            // ── Table migrations (CREATE IF NOT EXISTS) ───────────────────────
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS flexible_discount_credits (
                    credit_id        INT AUTO_INCREMENT PRIMARY KEY,
                    customer_id      INT NOT NULL,
                    discount_plan_id INT NOT NULL,
                    period_year      INT NOT NULL,
                    period_month     INT NOT NULL,
                    monthly_spend    DECIMAL(10,2) NOT NULL,
                    rate_applied     DECIMAL(5,2)  NOT NULL,
                    credit_amount    DECIMAL(10,2) NOT NULL,
                    remaining_credit DECIMAL(10,2) NOT NULL,
                    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (customer_id)      REFERENCES customer_accounts(customer_id)
                        ON DELETE CASCADE ON UPDATE CASCADE,
                    FOREIGN KEY (discount_plan_id) REFERENCES discount_plans(discount_plan_id)
                        ON DELETE CASCADE ON UPDATE CASCADE
                )
                """);

            // ── Column migrations (ADD IF NOT EXISTS) ─────────────────────────
            addColumnIfMissing(conn, "payment_reminders", "eligible_after", "DATETIME NULL");

            System.out.println("[DatabaseManager] Migrations applied.");
        } catch (SQLException e) {
            System.err.println("[DatabaseManager] Could not run migrations: " + e.getMessage());
        }
    }

    private static void addColumnIfMissing(Connection conn, String table, String column,
                                           String definition) throws SQLException {
        try (java.sql.ResultSet cols = conn.getMetaData().getColumns(null, null, table, column)) {
            if (cols.next()) return; // column already exists
        }
        String sql = "ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition;
        try (java.sql.Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("[DatabaseManager] Added column: " + table + "." + column);
        }
    }
}