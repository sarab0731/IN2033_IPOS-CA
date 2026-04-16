
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                new String[]{"Customer ID", "Full Name", "Status", "Balance £", "Reminder Type", "Reminder Status", "Invoice ID"},
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
        LocalDate today = app.TimeManager.today();

        // --- Phase 1: collect all unpaid invoices (close ResultSet before any writes) ---
        String sql = """
            SELECT i.customer_id, i.invoice_id, i.invoice_date, ca.account_status
            FROM invoices i
            JOIN customer_accounts ca ON i.customer_id = ca.customer_id
            WHERE i.status <> 'PAID'
            ORDER BY i.customer_id, i.invoice_date
            """;

        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("customer_id",    rs.getInt("customer_id"));
                row.put("invoice_id",     rs.getInt("invoice_id"));
                row.put("invoice_date",   rs.getString("invoice_date"));
                row.put("account_status", rs.getString("account_status"));
                rows.add(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        // --- Phase 2: process with a fresh connection ---
        // effectiveStatus tracks per-customer status changes made this run,
        // so a newer invoice can't downgrade a status set by an older invoice.
        Map<Integer, String> effectiveStatus = new HashMap<>();

        try (Connection conn = DatabaseManager.getConnection()) {
            for (Map<String, Object> row : rows) {
                int    customerId   = (int)    row.get("customer_id");
                int    invoiceId    = (int)    row.get("invoice_id");
                String dateStr      = (String) row.get("invoice_date");
                LocalDate invoiceDate = LocalDate.parse(dateStr.substring(0, 10));

                // Calendar-month boundaries:
                //   Invoices from month M are due end-of-M.
                //   SUSPENDED threshold  = 15th of M+1  (day after 15th triggers suspension)
                //   IN_DEFAULT threshold = last day of M+1
                LocalDate firstOfNextMonth  = invoiceDate.withDayOfMonth(1).plusMonths(1);
                LocalDate suspendedThreshold = firstOfNextMonth.withDayOfMonth(15);
                LocalDate defaultThreshold   = firstOfNextMonth.withDayOfMonth(
                        firstOfNextMonth.lengthOfMonth());

                String status = effectiveStatus.getOrDefault(customerId,
                        (String) row.get("account_status"));

                if (today.isAfter(defaultThreshold)) {
                    // Past end of M+1 → IN_DEFAULT + queue both reminders
                    if (!"IN_DEFAULT".equals(status)) {
                        CustomerDB.updateStatus(customerId, "IN_DEFAULT");
                        effectiveStatus.put(customerId, "IN_DEFAULT");
                    }
                    ensureReminderExists(conn, customerId, invoiceId, "FIRST",  null);
                    ensureReminderExists(conn, customerId, invoiceId, "SECOND", null);

                } else if (today.isAfter(suspendedThreshold)) {
                    // Past 15th of M+1 → SUSPENDED + queue first reminder
                    if ("ACTIVE".equals(status)) {
                        CustomerDB.updateStatus(customerId, "SUSPENDED");
                        effectiveStatus.put(customerId, "SUSPENDED");
                    }
                    ensureReminderExists(conn, customerId, invoiceId, "FIRST", null);
                }
                // Before 15th of M+1 → still within grace period, no action
                // Restoration (SUSPENDED→ACTIVE, IN_DEFAULT→ACTIVE) is handled
                // in CustomerPanel when payment is recorded, not here.
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Inserts a PENDING reminder if one does not already exist for this customer/invoice/type. */
    private void ensureReminderExists(Connection conn, int customerId, int invoiceId,
                                      String reminderType, java.sql.Timestamp eligibleAfter) throws Exception {
        String checkSql = "SELECT COUNT(*) FROM payment_reminders WHERE customer_id = ? AND invoice_id = ? AND reminder_type = ? AND reminder_status != 'NO_NEED'";
        String insertSql = "INSERT INTO payment_reminders (customer_id, invoice_id, reminder_type, reminder_status, eligible_after) VALUES (?, ?, ?, 'PENDING', ?)";

        try (PreparedStatement check = conn.prepareStatement(checkSql)) {
            check.setInt(1, customerId);
            check.setInt(2, invoiceId);
            check.setString(3, reminderType);
            ResultSet rs = check.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) return;
        }

        try (PreparedStatement insert = conn.prepareStatement(insertSql)) {
            insert.setInt(1, customerId);
            insert.setInt(2, invoiceId);
            insert.setString(3, reminderType);
            insert.setTimestamp(4, eligibleAfter); // null = immediately eligible
            insert.executeUpdate();
        }
    }

    private int sendDueReminders() {
        LocalDate today = app.TimeManager.today();

        // Collect all PENDING reminders that are now eligible (2nd reminders respect their eligible_after date)
        List<Map<String, Object>> due = new ArrayList<>();
        String fetchSql = """
            SELECT pr.reminder_id, pr.reminder_type, ca.customer_id, ca.full_name,
                   pr.invoice_id, i.invoice_number, i.amount_due
            FROM payment_reminders pr
            JOIN customer_accounts ca ON pr.customer_id = ca.customer_id
            JOIN invoices i           ON pr.invoice_id  = i.invoice_id
            WHERE pr.reminder_status = 'PENDING'
              AND (pr.eligible_after IS NULL OR pr.eligible_after <= ?)
            """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement fetch = conn.prepareStatement(fetchSql)) {
            fetch.setDate(1, java.sql.Date.valueOf(today));
            try (ResultSet rs = fetch.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("reminder_id",    rs.getInt("reminder_id"));
                    row.put("reminder_type",  rs.getString("reminder_type"));
                    row.put("customer_id",    rs.getInt("customer_id"));
                    row.put("full_name",      rs.getString("full_name"));
                    row.put("invoice_id",     rs.getInt("invoice_id"));
                    row.put("invoice_number", rs.getString("invoice_number"));
                    row.put("amount_due",     rs.getDouble("amount_due"));
                    due.add(row);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }

        if (due.isEmpty()) return 0;

        String updateSql = "UPDATE payment_reminders SET reminder_status = 'SENT', sent_at = ? WHERE reminder_id = ?";
        int count = 0;

        for (Map<String, Object> reminder : due) {
            String reminderType = (String) reminder.get("reminder_type");
            int    typeNum      = "FIRST".equals(reminderType) ? 1 : 2;
            String template     = TemplatesPanel.getReminderTemplate(typeNum);
            LocalDate paymentDate = today.plusDays(7);

            String content = template
                    .replace("{customer_name}",  (String) reminder.get("full_name"))
                    .replace("{invoice_number}", (String) reminder.get("invoice_number"))
                    .replace("{account_number}", String.valueOf(reminder.get("customer_id")))
                    .replace("{amount_due}",     String.format("%.2f", (Double) reminder.get("amount_due")))
                    .replace("{payment_date}",   paymentDate.toString())
                    .replace("{signed_by}",      "Pharmacy Manager");

            JTextArea area = new JTextArea(content);
            area.setEditable(false);
            area.setFont(new Font("Monospaced", Font.PLAIN, 12));
            area.setMargin(new java.awt.Insets(10, 10, 10, 10));
            area.setPreferredSize(new java.awt.Dimension(480, 280));

            JOptionPane.showMessageDialog(
                    this,
                    new JScrollPane(area),
                    reminderType + " Reminder — " + reminder.get("full_name"),
                    JOptionPane.INFORMATION_MESSAGE
            );

            // Mark this reminder as sent
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement update = conn.prepareStatement(updateSql)) {
                update.setTimestamp(1, java.sql.Timestamp.valueOf(app.TimeManager.now()));
                update.setInt(2, (int) reminder.get("reminder_id"));
                update.executeUpdate();
            } catch (Exception e) {
                e.printStackTrace();
            }

            count++;
        }

        return count;
    }

    private void loadTable() {
        tableModel.setRowCount(0);

        String sql = """
            SELECT ca.customer_id, ca.full_name, ca.account_status,
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
