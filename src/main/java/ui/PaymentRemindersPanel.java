package ui;

import database.CustomerDB;
import database.DatabaseManager;
import domain.Customer;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.List;

public class PaymentRemindersPanel extends JPanel {

    private DefaultTableModel tableModel;

    public PaymentRemindersPanel(ScreenRouter router) {
        setLayout(new BorderLayout());

        // --- HEADER ---
        JPanel header = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Payment Reminders", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 28));
        JButton backBtn = new JButton("Back");
        backBtn.addActionListener(e -> router.goTo(MainFrame.SCREEN_DASHBOARD));
        header.add(backBtn, BorderLayout.WEST);
        header.add(title, BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        // --- TABLE ---
        String[] cols = {"Customer ID", "Account No.", "Full Name", "Status",
                "Balance £", "1st Reminder", "2nd Reminder", "Invoice ID"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        add(new JScrollPane(table), BorderLayout.CENTER);

        // --- BUTTONS ---
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 8));
        JButton generateBtn = new JButton("Generate Reminders");
        generateBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        JButton refreshBtn  = new JButton("Refresh");
        buttons.add(generateBtn);
        buttons.add(refreshBtn);
        add(buttons, BorderLayout.SOUTH);

        refreshBtn.addActionListener(e -> loadTable());

        /*
         * GENERATE REMINDERS — implements the pseudo-code from brief page 18 exactly:
         *
         * if (status_1stReminder = 'due')
         *   generate(1st Reminder)
         *   status_1stReminder = 'sent'
         *   date_2ndReminder = current_date + 15 days
         *
         * if (status_2ndReminder = 'due')
         *   if (date_2ndReminder <= current_date)
         *     generate(2nd Reminder)
         *     status_2ndReminder = 'sent'
         */
        generateBtn.addActionListener(e -> {
            int generated = runReminderAlgorithm();
            JOptionPane.showMessageDialog(this,
                    generated == 0
                            ? "No reminders due at this time."
                            : generated + " reminder(s) generated.");
            loadTable();
        });

        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentShown(java.awt.event.ComponentEvent e) {
                // auto-update account statuses before showing
                updateAccountStatuses();
                loadTable();
            }
        });
    }

    // ----------------------------------------------------------------
    // Implements reminder algorithm exactly as in brief page 18
    // ----------------------------------------------------------------
    private int runReminderAlgorithm() {
        int count = 0;
        LocalDate today = LocalDate.now();

        String fetchSql = """
            SELECT pr.reminder_id, pr.customer_id, pr.invoice_id,
                   pr.reminder_type, pr.reminder_status, pr.sent_at,
                   ca.full_name, ca.account_number, ca.email,
                   i.amount_due, i.invoice_number
            FROM payment_reminders pr
            JOIN customer_accounts ca ON pr.customer_id = ca.customer_id
            JOIN invoices i           ON pr.invoice_id  = i.invoice_id
            WHERE pr.reminder_status = 'PENDING'
            """;

        String updateSql = """
            UPDATE payment_reminders
            SET reminder_status = 'SENT', sent_at = CURRENT_TIMESTAMP
            WHERE reminder_id = ?
            """;

        String scheduleSecondSql = """
            UPDATE payment_reminders
            SET reminder_status = 'PENDING'
            WHERE invoice_id = ? AND reminder_type = 'SECOND' AND reminder_status = 'PENDING'
            """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement fetch  = conn.prepareStatement(fetchSql);
             PreparedStatement update = conn.prepareStatement(updateSql)) {

            ResultSet rs = fetch.executeQuery();

            while (rs.next()) {
                String type   = rs.getString("reminder_type");
                int reminderId = rs.getInt("reminder_id");
                String sentAt  = rs.getString("sent_at");

                if ("FIRST".equals(type)) {
                    // status_1stReminder = 'due' → generate 1st reminder
                    showReminderDialog(
                            rs.getString("full_name"),
                            rs.getString("account_number"),
                            rs.getString("invoice_number"),
                            rs.getDouble("amount_due"),
                            "FIRST",
                            today.plusDays(7).toString()
                    );

                    // status_1stReminder = 'sent'
                    update.setInt(1, reminderId);
                    update.executeUpdate();

                    // schedule 2nd reminder: date_2ndReminder = current_date + 15 days
                    scheduleSecondReminder(conn, rs.getInt("invoice_id"),
                            rs.getInt("customer_id"), today.plusDays(15));

                    count++;

                } else if ("SECOND".equals(type)) {
                    // if (date_2ndReminder <= current_date)
                    if (sentAt == null || LocalDate.parse(sentAt.substring(0, 10))
                            .compareTo(today) <= 0) {

                        showReminderDialog(
                                rs.getString("full_name"),
                                rs.getString("account_number"),
                                rs.getString("invoice_number"),
                                rs.getDouble("amount_due"),
                                "SECOND",
                                today.plusDays(7).toString()
                        );

                        // status_2ndReminder = 'sent'
                        update.setInt(1, reminderId);
                        update.executeUpdate();

                        count++;
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return count;
    }

    private void scheduleSecondReminder(Connection conn, int invoiceId,
                                        int customerId, LocalDate scheduledDate) {
        // check if a second reminder already exists for this invoice
        String checkSql = """
            SELECT COUNT(*) FROM payment_reminders
            WHERE invoice_id = ? AND reminder_type = 'SECOND'
            """;

        String insertSql = """
            INSERT INTO payment_reminders
                (customer_id, invoice_id, reminder_type, reminder_status, sent_at)
            VALUES (?, ?, 'SECOND', 'PENDING', ?)
            """;

        try {
            PreparedStatement check = conn.prepareStatement(checkSql);
            check.setInt(1, invoiceId);
            ResultSet rs = check.executeQuery();
            rs.next();
            if (rs.getInt(1) > 0) return; // already scheduled

            PreparedStatement insert = conn.prepareStatement(insertSql);
            insert.setInt(1, customerId);
            insert.setInt(2, invoiceId);
            insert.setString(3, scheduledDate.toString());
            insert.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * Auto-updates account statuses based on time rules from brief:
     * - 15 days after month end with unpaid balance → SUSPENDED + set 1st reminder due
     * - 30 days after month end with unpaid balance → IN_DEFAULT + set 2nd reminder due
     */
    private void updateAccountStatuses() {
        LocalDate today = LocalDate.now();

        String sql = """
            SELECT i.customer_id, i.invoice_id, i.invoice_date, i.amount_due, i.status,
                   ca.account_status
            FROM invoices i
            JOIN customer_accounts ca ON i.customer_id = ca.customer_id
            WHERE i.status NOT IN ('PAID')
            """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                LocalDate invoiceDate = LocalDate.parse(rs.getString("invoice_date").substring(0, 10));
                long daysPast = java.time.temporal.ChronoUnit.DAYS.between(invoiceDate, today);
                int customerId = rs.getInt("customer_id");
                int invoiceId  = rs.getInt("invoice_id");
                String currentStatus = rs.getString("account_status");

                if (daysPast >= 30 && !"IN_DEFAULT".equals(currentStatus)) {
                    CustomerDB.updateStatus(customerId, "IN_DEFAULT");
                    ensureReminderExists(conn, customerId, invoiceId, "SECOND");

                } else if (daysPast >= 15 && "ACTIVE".equals(currentStatus)) {
                    CustomerDB.updateStatus(customerId, "SUSPENDED");
                    ensureReminderExists(conn, customerId, invoiceId, "FIRST");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void ensureReminderExists(Connection conn, int customerId,
                                      int invoiceId, String type) {
        String checkSql = """
            SELECT COUNT(*) FROM payment_reminders
            WHERE invoice_id = ? AND reminder_type = ?
            """;
        String insertSql = """
            INSERT INTO payment_reminders (customer_id, invoice_id, reminder_type, reminder_status)
            VALUES (?, ?, ?, 'PENDING')
            """;

        try {
            PreparedStatement check = conn.prepareStatement(checkSql);
            check.setInt(1, invoiceId);
            check.setString(2, type);
            ResultSet rs = check.executeQuery();
            rs.next();
            if (rs.getInt(1) > 0) return;

            PreparedStatement insert = conn.prepareStatement(insertSql);
            insert.setInt(1, customerId);
            insert.setInt(2, invoiceId);
            insert.setString(3, type);
            insert.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showReminderDialog(String name, String accountNumber, String invoiceNumber,
                                    double amount, String type, String payByDate) {
        String heading  = "FIRST".equals(type) ? "REMINDER" : "SECOND REMINDER";
        String message  = String.format(
                "%s\n\nInvoice No.: %s\nAccount: %s\nClient: %s\n\n" +
                        "According to our records, we have not yet received payment of £%.2f.\n" +
                        "Please make payment by: %s\n\n" +
                        "If you have already sent payment, please disregard this notice.",
                heading, invoiceNumber, accountNumber, name, amount, payByDate
        );

        JOptionPane.showMessageDialog(this, message,
                heading + " — " + name, JOptionPane.INFORMATION_MESSAGE);
    }

    private void loadTable() {
        tableModel.setRowCount(0);

        String sql = """
            SELECT pr.customer_id, ca.account_number, ca.full_name,
                   ca.account_status, ca.current_balance,
                   pr.reminder_type, pr.reminder_status, pr.invoice_id
            FROM payment_reminders pr
            JOIN customer_accounts ca ON pr.customer_id = ca.customer_id
            ORDER BY ca.full_name, pr.reminder_type
            """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        rs.getInt("customer_id"),
                        rs.getString("account_number"),
                        rs.getString("full_name"),
                        rs.getString("account_status"),
                        String.format("%.2f", rs.getDouble("current_balance")),
                        rs.getString("reminder_type"),
                        rs.getString("reminder_status"),
                        rs.getInt("invoice_id")
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}