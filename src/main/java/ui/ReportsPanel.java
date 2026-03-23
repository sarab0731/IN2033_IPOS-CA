package ui;

import database.CustomerDB;
import database.DatabaseManager;
import database.ProductDB;
import domain.Customer;
import domain.Product;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.List;

public class ReportsPanel extends JPanel {

    public ReportsPanel(ScreenRouter router) {
        setLayout(new BorderLayout());

        // --- HEADER ---
        JPanel header = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Reports", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 28));
        JButton backBtn = new JButton("Back");
        backBtn.addActionListener(e -> router.goTo(MainFrame.SCREEN_DASHBOARD));
        header.add(backBtn, BorderLayout.WEST);
        header.add(title, BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        // --- TABS ---
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Turnover",           buildTurnoverTab());
        tabs.addTab("Stock Availability", buildStockTab());
        tabs.addTab("Customer Debt",      buildDebtTab());
        add(tabs, BorderLayout.CENTER);
    }

    // ----------------------------------------------------------------
    // TAB 1 — Sales turnover for a given period
    // ----------------------------------------------------------------
    private JPanel buildTurnoverTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // date filter row
        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        filterRow.add(new JLabel("From (YYYY-MM-DD):"));
        JTextField fromField = new JTextField("2024-01-01", 12);
        filterRow.add(fromField);
        filterRow.add(new JLabel("To (YYYY-MM-DD):"));
        JTextField toField = new JTextField("2099-12-31", 12);
        filterRow.add(toField);
        JButton runBtn = new JButton("Generate");
        filterRow.add(runBtn);
        panel.add(filterRow, BorderLayout.NORTH);

        // results table
        String[] cols = {"Item ID", "Description", "Qty Sold", "Revenue £"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JLabel totalLabel = new JLabel(" ", SwingConstants.RIGHT);
        totalLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        totalLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 8));
        panel.add(totalLabel, BorderLayout.SOUTH);

        runBtn.addActionListener(e -> {
            model.setRowCount(0);
            String sql = """
                SELECT p.item_id, p.description,
                       SUM(si.quantity) AS qty_sold,
                       SUM(si.line_total) AS revenue
                FROM sale_items si
                JOIN products p ON si.product_id = p.product_id
                JOIN sales s    ON si.sale_id     = s.sale_id
                WHERE DATE(s.sale_datetime) BETWEEN ? AND ?
                GROUP BY p.product_id
                ORDER BY revenue DESC
                """;

            double grandTotal = 0;
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, fromField.getText().trim());
                stmt.setString(2, toField.getText().trim());
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    double revenue = rs.getDouble("revenue");
                    grandTotal += revenue;
                    model.addRow(new Object[]{
                            rs.getString("item_id"),
                            rs.getString("description"),
                            rs.getInt("qty_sold"),
                            String.format("%.2f", revenue)
                    });
                }

            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(panel, "Error generating report.");
            }

            totalLabel.setText("Total Revenue: £" + String.format("%.2f", grandTotal));
        });

        return panel;
    }

    // ----------------------------------------------------------------
    // TAB 2 — Stock availability report
    // ----------------------------------------------------------------
    private JPanel buildStockTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        JButton refreshBtn = new JButton("Refresh");
        JCheckBox lowStockOnly = new JCheckBox("Show low stock only");
        topRow.add(refreshBtn);
        topRow.add(lowStockOnly);
        panel.add(topRow, BorderLayout.NORTH);

        String[] cols = {"Item ID", "Description", "Stock Qty", "Min Level",
                "Price £", "VAT %", "Stock Value £", "Status"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JLabel totalLabel = new JLabel(" ", SwingConstants.RIGHT);
        totalLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        totalLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 8));
        panel.add(totalLabel, BorderLayout.SOUTH);

        Runnable load = () -> {
            model.setRowCount(0);
            List<Product> products = lowStockOnly.isSelected()
                    ? ProductDB.getLowStockProducts()
                    : ProductDB.getAllProducts();

            double totalValue = 0;
            for (Product p : products) {
                double stockValue = p.getPrice() * p.getStockQuantity()
                        * (1 + p.getVatRate() / 100);
                totalValue += stockValue;
                model.addRow(new Object[]{
                        p.getItemId(),
                        p.getDescription(),
                        p.getStockQuantity(),
                        p.getMinStockLevel(),
                        String.format("%.2f", p.getPrice()),
                        String.format("%.2f", p.getVatRate()),
                        String.format("%.2f", stockValue),
                        p.isLowStock() ? "LOW STOCK" : "OK"
                });
            }
            totalLabel.setText("Total Stock Value (inc. VAT): £" + String.format("%.2f", totalValue));
        };

        refreshBtn.addActionListener(e -> load.run());
        lowStockOnly.addActionListener(e -> load.run());

        // load on show
        panel.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentShown(java.awt.event.ComponentEvent e) { load.run(); }
        });

        return panel;
    }

    // ----------------------------------------------------------------
    // TAB 3 — Aggregated customer debt report
    // as per brief: debt at start of period, payments received, debt at end
    // ----------------------------------------------------------------
    private JPanel buildDebtTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        filterRow.add(new JLabel("From (YYYY-MM-DD):"));
        JTextField fromField = new JTextField("2024-01-01", 12);
        filterRow.add(fromField);
        filterRow.add(new JLabel("To (YYYY-MM-DD):"));
        JTextField toField = new JTextField("2099-12-31", 12);
        filterRow.add(toField);
        JButton runBtn = new JButton("Generate");
        filterRow.add(runBtn);
        panel.add(filterRow, BorderLayout.NORTH);

        String[] cols = {"Account No.", "Full Name", "Status",
                "Opening Debt £", "Payments Received £", "Current Balance £"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JLabel summaryLabel = new JLabel(" ", SwingConstants.RIGHT);
        summaryLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        summaryLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 8));
        panel.add(summaryLabel, BorderLayout.SOUTH);

        runBtn.addActionListener(e -> {
            model.setRowCount(0);

            // payments received in period per customer
            String paymentsSql = """
                SELECT customer_id, SUM(amount) AS total_paid
                FROM account_payments
                WHERE DATE(payment_date) BETWEEN ? AND ?
                GROUP BY customer_id
                """;

            // invoices raised in period per customer (opening debt)
            String invoicesSql = """
                SELECT customer_id, SUM(amount_due) AS total_invoiced
                FROM invoices
                WHERE DATE(invoice_date) < ?
                GROUP BY customer_id
                """;

            try (Connection conn = DatabaseManager.getConnection()) {
                PreparedStatement payStmt = conn.prepareStatement(paymentsSql);
                payStmt.setString(1, fromField.getText().trim());
                payStmt.setString(2, toField.getText().trim());
                ResultSet payRs = payStmt.executeQuery();

                java.util.Map<Integer, Double> payments = new java.util.HashMap<>();
                while (payRs.next()) {
                    payments.put(payRs.getInt("customer_id"), payRs.getDouble("total_paid"));
                }

                PreparedStatement invStmt = conn.prepareStatement(invoicesSql);
                invStmt.setString(1, fromField.getText().trim());
                ResultSet invRs = invStmt.executeQuery();

                java.util.Map<Integer, Double> openingDebt = new java.util.HashMap<>();
                while (invRs.next()) {
                    openingDebt.put(invRs.getInt("customer_id"), invRs.getDouble("total_invoiced"));
                }

                List<Customer> customers = CustomerDB.getAllActiveCustomers();
                double totalOpeningDebt   = 0;
                double totalPayments      = 0;
                double totalCurrentBalance = 0;

                for (Customer c : customers) {
                    double opening  = openingDebt.getOrDefault(c.getCustomerId(), 0.0);
                    double paid     = payments.getOrDefault(c.getCustomerId(), 0.0);
                    double balance  = c.getCurrentBalance();

                    totalOpeningDebt    += opening;
                    totalPayments       += paid;
                    totalCurrentBalance += balance;

                    model.addRow(new Object[]{
                            c.getAccountNumber(),
                            c.getFullName(),
                            c.getAccountStatus(),
                            String.format("%.2f", opening),
                            String.format("%.2f", paid),
                            String.format("%.2f", balance)
                    });
                }

                summaryLabel.setText(
                        "Opening Debt: £" + String.format("%.2f", totalOpeningDebt)
                                + "   |   Payments: £" + String.format("%.2f", totalPayments)
                                + "   |   Current Total Balance: £" + String.format("%.2f", totalCurrentBalance)
                );

            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(panel, "Error generating report.");
            }
        });

        return panel;
    }
}