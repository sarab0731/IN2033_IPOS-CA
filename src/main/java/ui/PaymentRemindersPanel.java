
package ui;

import database.CustomerDB;
import database.DatabaseManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;

public class PaymentRemindersPanel extends JPanel implements ThemeManager.ThemeListener {

    private final ScreenRouter router;

    private JPanel contentPanel;
    private JPanel headerPanel;
    private JPanel tableCard;

    private JLabel reminderQueueLabel;

    private JTable table;
    private JScrollPane scrollPane;
    private DefaultTableModel tableModel;

    private JButton generateRemindersBtn;
    private JButton refreshBtn;

    public PaymentRemindersPanel(ScreenRouter router) {
        this.router = router;

        setLayout(new BorderLayout());
        ThemeManager.register(this);

        AppShell shell = new AppShell(
                router,
                MainFrame.SCREEN_REMINDERS,
                "Payment Reminders",
                "Track overdue balances and reminder status",
                buildContent()
        );

        add(shell, BorderLayout.CENTER);
        wireActions();
        loadTable();
        applyTheme();
    }

    private JPanel buildContent() {
        contentPanel = new JPanel(new BorderLayout(20, 20));
        contentPanel.setBorder(new EmptyBorder(8, 8, 8, 8));

        headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        reminderQueueLabel = new JLabel("Reminder Queue");
        reminderQueueLabel.setFont(new Font("SansSerif", Font.BOLD, 16));

        JPanel rightButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        rightButtons.setOpaque(false);

        generateRemindersBtn = new JButton("Generate Reminders");
        refreshBtn = new JButton("Refresh");

        rightButtons.add(generateRemindersBtn);
        rightButtons.add(refreshBtn);

        headerPanel.add(reminderQueueLabel, BorderLayout.WEST);
        headerPanel.add(rightButtons, BorderLayout.EAST);

        tableCard = AppShell.createCard();
        tableCard.setLayout(new BorderLayout());

        tableModel = new DefaultTableModel(
                new String[]{"Customer ID", "Account No.", "Full Name", "Status", "Balance £", "Reminder Type", "Reminder Status", "Invoice ID"},
                0
        ) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };

        table = new JTable(tableModel);

        configureTable(table);

        scrollPane = new JScrollPane(table);
        styleScrollPane(scrollPane);

        tableCard.add(scrollPane, BorderLayout.CENTER);

        contentPanel.add(headerPanel, BorderLayout.NORTH);
        contentPanel.add(tableCard, BorderLayout.CENTER);

        return contentPanel;
    }

    private void wireActions() {
        refreshBtn.addActionListener(e -> loadTable());
        generateRemindersBtn.addActionListener(e -> {
            updateAccountStatuses();
            int generated = sendDueReminders();
            loadTable();
            JOptionPane.showMessageDialog(
                    this,
                    generated == 0 ? "No reminders were due." : generated + " reminder(s) generated."
            );
        });

        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                updateAccountStatuses();
                loadTable();
            }
        });
    }

    private void updateAccountStatuses() {
        LocalDate today = LocalDate.now();

        String sql = """
            SELECT i.customer_id, i.invoice_id, i.invoice_date, ca.account_status
            FROM invoices i
            JOIN customer_accounts ca ON i.customer_id = ca.customer_id
            WHERE i.status <> 'PAID'
            """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                LocalDate invoiceDate = LocalDate.parse(rs.getString("invoice_date").substring(0, 10));
                long daysPast = java.time.temporal.ChronoUnit.DAYS.between(invoiceDate, today);
                int customerId = rs.getInt("customer_id");
                int invoiceId = rs.getInt("invoice_id");
                String accountStatus = rs.getString("account_status");

                if (daysPast >= 30 && !"IN_DEFAULT".equals(accountStatus)) {
                    CustomerDB.updateStatus(customerId, "IN_DEFAULT");
                    ensureReminderExists(conn, customerId, invoiceId, "SECOND");
                }
                if (daysPast < 15 && "SUSPENDED".equals(accountStatus)) {
                    // Check if balance is now 0
                    String balSql = "SELECT current_balance FROM customer_accounts WHERE customer_id = ?";
                    try (PreparedStatement balStmt = conn.prepareStatement(balSql)) {
                        balStmt.setInt(1, customerId);
                        ResultSet balRs = balStmt.executeQuery();
                        if (balRs.next() && balRs.getDouble("current_balance") == 0) {
                            CustomerDB.updateStatus(customerId, "ACTIVE");
                        }
                    }
                }
                else if (daysPast >= 15 && "ACTIVE".equals(accountStatus)) {
                    CustomerDB.updateStatus(customerId, "SUSPENDED");
                    ensureReminderExists(conn, customerId, invoiceId, "FIRST");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void ensureReminderExists(Connection conn, int customerId, int invoiceId, String reminderType) throws Exception {
        String checkSql = "SELECT COUNT(*) FROM payment_reminders WHERE customer_id = ? AND invoice_id = ? AND reminder_type = ?";
        String insertSql = """
            INSERT INTO payment_reminders (customer_id, invoice_id, reminder_type, reminder_status)
            VALUES (?, ?, ?, 'PENDING')
            """;

        try (PreparedStatement check = conn.prepareStatement(checkSql)) {
            check.setInt(1, customerId);
            check.setInt(2, invoiceId);
            check.setString(3, reminderType);
            ResultSet rs = check.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                return;
            }
        }

        try (PreparedStatement insert = conn.prepareStatement(insertSql)) {
            insert.setInt(1, customerId);
            insert.setInt(2, invoiceId);
            insert.setString(3, reminderType);
            insert.executeUpdate();
        }
    }

    private int sendDueReminders() {
        int count = 0;

        String fetchSql = """
            SELECT pr.reminder_id, pr.reminder_type, ca.full_name, ca.account_number, i.invoice_number, i.amount_due
            FROM payment_reminders pr
            JOIN customer_accounts ca ON pr.customer_id = ca.customer_id
            JOIN invoices i ON pr.invoice_id = i.invoice_id
            WHERE pr.reminder_status = 'PENDING'
            """;

        String updateSql = """
            UPDATE payment_reminders
            SET reminder_status = 'SENT', sent_at = CURRENT_TIMESTAMP
            WHERE reminder_id = ?
            """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement fetch = conn.prepareStatement(fetchSql);
             PreparedStatement update = conn.prepareStatement(updateSql);
             ResultSet rs = fetch.executeQuery()) {

            while (rs.next()) {
                int reminderType = "FIRST".equals(rs.getString("reminder_type")) ? 1 : 2;
                String template  = TemplatesPanel.getReminderTemplate(reminderType);

                String content = template
                        .replace("{customer_name}",  rs.getString("full_name"))
                        .replace("{invoice_number}", rs.getString("invoice_number"))
                        .replace("{account_number}", rs.getString("account_number"))
                        .replace("{amount_due}",     String.format("%.2f", rs.getDouble("amount_due")))
                        .replace("{signed_by}",      "Pharmacy Manager");

                JTextArea area = new JTextArea(content);
                area.setEditable(false);
                area.setFont(new Font("Monospaced", Font.PLAIN, 12));
                area.setMargin(new java.awt.Insets(10, 10, 10, 10));
                area.setPreferredSize(new java.awt.Dimension(480, 280));

                JOptionPane.showMessageDialog(
                        this,
                        new javax.swing.JScrollPane(area),
                        rs.getString("reminder_type") + " Reminder — " + rs.getString("full_name"),
                        JOptionPane.INFORMATION_MESSAGE
                );

                update.setInt(1, rs.getInt("reminder_id"));
                update.executeUpdate();
                count++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return count;
    }

    private void loadTable() {
        tableModel.setRowCount(0);

        String sql = """
            SELECT ca.customer_id, ca.account_number, ca.full_name, ca.account_status,
                   ca.current_balance, pr.reminder_type, pr.reminder_status, pr.invoice_id
            FROM payment_reminders pr
            JOIN customer_accounts ca ON pr.customer_id = ca.customer_id
            ORDER BY pr.reminder_status, ca.full_name
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

    private void configureTable(JTable table) {
        table.setRowHeight(44);
        table.setShowGrid(true);
        table.setIntercellSpacing(new Dimension(1, 1));
        table.setFillsViewportHeight(true);
        table.setBorder(BorderFactory.createEmptyBorder());
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setDefaultEditor(Object.class, null);

        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setFont(new Font("SansSerif", Font.BOLD, 13));
        header.setBorder(BorderFactory.createEmptyBorder());

        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.setHorizontalAlignment(SwingConstants.LEFT);
        renderer.setBorder(new EmptyBorder(0, 10, 0, 10));
        table.setDefaultRenderer(Object.class, renderer);
    }

    private void styleScrollPane(JScrollPane sp) {
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setOpaque(true);
        sp.setBackground(ThemeManager.tableBackground());
        sp.getViewport().setBackground(ThemeManager.tableBackground());
        sp.getViewport().setBorder(null);
    }

    private void applyTableTheme(JTable table) {
        table.setBackground(ThemeManager.tableBackground());
        table.setForeground(ThemeManager.textPrimary());
        table.setGridColor(ThemeManager.tableGrid());
        table.setSelectionBackground(ThemeManager.selectionBackground());
        table.setSelectionForeground(ThemeManager.textPrimary());

        JTableHeader header = table.getTableHeader();
        if (header != null) {
            header.setBackground(ThemeManager.tableHeaderBackground());
            header.setForeground(ThemeManager.textPrimary());
            header.setBorder(BorderFactory.createEmptyBorder());
            header.setOpaque(true);
        }

        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.setBackground(ThemeManager.tableBackground());
        renderer.setForeground(ThemeManager.textPrimary());
        renderer.setHorizontalAlignment(SwingConstants.LEFT);
        renderer.setBorder(new EmptyBorder(0, 10, 0, 10));
        table.setDefaultRenderer(Object.class, renderer);
    }

    private void stylePrimaryButton(JButton button) {
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
        button.setFont(new Font("SansSerif", Font.BOLD, 13));
        button.setBackground(ThemeManager.buttonDark());
        button.setForeground(ThemeManager.textLight());
    }

    private void styleSecondaryButton(JButton button) {
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createLineBorder(ThemeManager.borderColor()));
        button.setFont(new Font("SansSerif", Font.BOLD, 13));
        button.setBackground(ThemeManager.buttonLight());
        button.setForeground(ThemeManager.textPrimary());
    }

    @Override
    public void applyTheme() {
        setBackground(ThemeManager.appBackground());

        if (contentPanel != null) contentPanel.setBackground(ThemeManager.appBackground());
        if (headerPanel != null) headerPanel.setBackground(ThemeManager.appBackground());
        if (tableCard != null) tableCard.setBackground(ThemeManager.panelBackground());

        if (reminderQueueLabel != null) reminderQueueLabel.setForeground(ThemeManager.textPrimary());

        if (table != null) applyTableTheme(table);
        if (scrollPane != null) styleScrollPane(scrollPane);

        if (generateRemindersBtn != null) stylePrimaryButton(generateRemindersBtn);
        if (refreshBtn != null) styleSecondaryButton(refreshBtn);

        repaint();
        revalidate();
    }
}
