package ui;

import database.CustomerDB;
import database.DatabaseManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;

public class PaymentRemindersPanel extends JPanel {

    private DefaultTableModel tableModel;
    private JTable table;

    public PaymentRemindersPanel(ScreenRouter router) {
        setLayout(new BorderLayout());
        setBackground(new Color(242, 242, 242));

        JPanel reminderContent = buildReminderContent();

        AppShell shell = new AppShell(
                router,
                MainFrame.SCREEN_REMINDERS,
                "Payment Reminders",
                "Track overdue balances and reminder status",
                reminderContent
        );

        add(shell, BorderLayout.CENTER);

        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                updateAccountStatuses();
                loadTable();
            }
        });
    }

    private JPanel buildReminderContent() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setOpaque(false);

        JPanel card = AppShell.createCard();
        card.setLayout(new BorderLayout(0, 18));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(232, 232, 232)),
                new EmptyBorder(20, 20, 20, 20)
        ));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JLabel title = new JLabel("Reminder Queue");
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        title.setForeground(new Color(35, 35, 35));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);

        JButton generateBtn = buildDarkButton("Generate Reminders");
        JButton refreshBtn = buildLightButton("Refresh");

        generateBtn.addActionListener(e -> {
            int generated = runReminderAlgorithm();
            JOptionPane.showMessageDialog(
                    this,
                    generated == 0 ? "No reminders due at this time." : generated + " reminder(s) generated."
            );
            loadTable();
        });

        refreshBtn.addActionListener(e -> {
            updateAccountStatuses();
            loadTable();
        });

        right.add(generateBtn);
        right.add(refreshBtn);

        top.add(title, BorderLayout.WEST);
        top.add(right, BorderLayout.EAST);

        String[] cols = {"Customer ID", "Account No.", "Full Name", "Status", "Balance £", "Reminder Type", "Reminder Status", "Invoice ID"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        table = new JTable(tableModel);
        styleTable(table);
        table.getColumnModel().getColumn(3).setCellRenderer(new StatusCellRenderer());
        table.getColumnModel().getColumn(6).setCellRenderer(new ReminderStatusCellRenderer());

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(ThemeManager.borderColor()));
        scrollPane.getViewport().setBackground(ThemeManager.tableBackground());
        scrollPane.setBackground(ThemeManager.tableBackground());

        card.add(top, BorderLayout.NORTH);
        card.add(scrollPane, BorderLayout.CENTER);

        outer.add(card, BorderLayout.CENTER);
        return outer;
    }

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

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement fetch = conn.prepareStatement(fetchSql);
             PreparedStatement update = conn.prepareStatement(updateSql)) {

            ResultSet rs = fetch.executeQuery();

            while (rs.next()) {
                String type = rs.getString("reminder_type");
                int reminderId = rs.getInt("reminder_id");
                String sentAt = rs.getString("sent_at");

                if ("FIRST".equals(type)) {
                    showReminderDialog(
                            rs.getString("full_name"),
                            rs.getString("account_number"),
                            rs.getString("invoice_number"),
                            rs.getDouble("amount_due"),
                            "FIRST",
                            today.plusDays(7).toString()
                    );

                    update.setInt(1, reminderId);
                    update.executeUpdate();

                    scheduleSecondReminder(conn, rs.getInt("invoice_id"), rs.getInt("customer_id"), today.plusDays(15));
                    count++;

                } else if ("SECOND".equals(type)) {
                    if (sentAt == null || LocalDate.parse(sentAt.substring(0, 10)).compareTo(today) <= 0) {
                        showReminderDialog(
                                rs.getString("full_name"),
                                rs.getString("account_number"),
                                rs.getString("invoice_number"),
                                rs.getDouble("amount_due"),
                                "SECOND",
                                today.plusDays(7).toString()
                        );

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

    private void scheduleSecondReminder(Connection conn, int invoiceId, int customerId, LocalDate scheduledDate) {
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
            if (rs.getInt(1) > 0) {
                return;
            }

            PreparedStatement insert = conn.prepareStatement(insertSql);
            insert.setInt(1, customerId);
            insert.setInt(2, invoiceId);
            insert.setString(3, scheduledDate.toString());
            insert.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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
                int invoiceId = rs.getInt("invoice_id");
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

    private void ensureReminderExists(Connection conn, int customerId, int invoiceId, String type) {
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
            if (rs.getInt(1) > 0) {
                return;
            }

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
        String heading = "FIRST".equals(type) ? "REMINDER" : "SECOND REMINDER";
        String message = String.format(
                "%s\n\nInvoice No.: %s\nAccount: %s\nClient: %s\n\n" +
                        "According to our records, we have not yet received payment of £%.2f.\n" +
                        "Please make payment by: %s\n\n" +
                        "If you have already sent payment, please disregard this notice.",
                heading, invoiceNumber, accountNumber, name, amount, payByDate
        );

        JOptionPane.showMessageDialog(
                this,
                message,
                heading + " — " + name,
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void loadTable() {
        if (tableModel == null) {
            return;
        }

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

    private void styleTable(JTable table) {
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(38);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFont(new Font("SansSerif", Font.PLAIN, 12));
        table.setBackground(ThemeManager.tableBackground());
        table.setForeground(ThemeManager.textPrimary());
        table.setSelectionBackground(ThemeManager.selectionBackground());
        table.setSelectionForeground(ThemeManager.textPrimary());
        table.setGridColor(ThemeManager.tableGrid());
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.getTableHeader().setBackground(ThemeManager.tableHeaderBackground());
        table.getTableHeader().setForeground(ThemeManager.textPrimary());
        table.getTableHeader().setReorderingAllowed(false);
    }

    private JButton buildDarkButton(String text) {
        JButton btn = new JButton(text);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(true);
        btn.setOpaque(true);
        btn.setBackground(new Color(30, 32, 38));
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(9, 16, 9, 16));
        return btn;
    }

    private JButton buildLightButton(String text) {
        JButton btn = new JButton(text);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(true);
        btn.setOpaque(true);
        btn.setBackground(Color.WHITE);
        btn.setForeground(new Color(45, 45, 45));
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)),
                new EmptyBorder(9, 16, 9, 16)
        ));
        return btn;
    }

    private static class StatusCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column
        ) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column
            );

            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setFont(new Font("SansSerif", Font.BOLD, 12));

            String status = value == null ? "" : value.toString();

            if (!isSelected) {
                label.setForeground(Color.BLACK);

                switch (status.toUpperCase()) {
                    case "ACTIVE" -> label.setBackground(new Color(201, 224, 98));
                    case "SUSPENDED" -> label.setBackground(new Color(240, 205, 90));
                    case "IN_DEFAULT" -> label.setBackground(new Color(234, 105, 90));
                    default -> label.setBackground(Color.WHITE);
                }
            }

            return label;
        }
    }

    private static class ReminderStatusCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column
        ) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column
            );

            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setFont(new Font("SansSerif", Font.BOLD, 12));

            String status = value == null ? "" : value.toString();

            if (!isSelected) {
                label.setForeground(Color.BLACK);

                switch (status.toUpperCase()) {
                    case "PENDING" -> label.setBackground(new Color(240, 205, 90));
                    case "SENT" -> label.setBackground(new Color(201, 224, 98));
                    default -> label.setBackground(Color.WHITE);
                }
            }

            return label;
        }
    }
}