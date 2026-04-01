package ui;

import database.CustomerDB;
import database.DatabaseManager;
import database.ProductDB;
import domain.Customer;
import domain.Product;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

public class ReportsPanel extends JPanel {

    public ReportsPanel(ScreenRouter router) {
        setLayout(new BorderLayout());
        setBackground(new Color(242, 242, 242));

        JPanel reportsContent = buildReportsContent();

        AppShell shell = new AppShell(
                router,
                MainFrame.SCREEN_REPORTS,
                "Reports",
                "Generate sales, stock, and debt reports",
                reportsContent
        );

        add(shell, BorderLayout.CENTER);
    }

    private JPanel buildReportsContent() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setOpaque(false);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Turnover", buildTurnoverTab());
        tabs.addTab("Stock Availability", buildStockTab());
        tabs.addTab("Customer Debt", buildDebtTab());

        outer.add(tabs, BorderLayout.CENTER);
        return outer;
    }

    private JPanel buildTurnoverTab() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setOpaque(false);

        JPanel card = AppShell.createCard();
        card.setLayout(new BorderLayout(0, 18));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(232, 232, 232)),
                new EmptyBorder(20, 20, 20, 20)
        ));

        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        filterRow.setOpaque(false);

        JTextField fromField = buildTextField("2024-01-01", 12);
        JTextField toField = buildTextField("2099-12-31", 12);
        JButton runBtn = buildDarkButton("Generate");

        filterRow.add(new JLabel("From (YYYY-MM-DD):"));
        filterRow.add(fromField);
        filterRow.add(new JLabel("To (YYYY-MM-DD):"));
        filterRow.add(toField);
        filterRow.add(runBtn);

        String[] cols = {"Item ID", "Description", "Qty Sold", "Revenue £"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        JTable table = new JTable(model);
        styleTable(table);

        JLabel totalLabel = new JLabel(" ", SwingConstants.RIGHT);
        totalLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        totalLabel.setBorder(new EmptyBorder(4, 0, 4, 8));

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

                totalLabel.setText("Total Revenue: £" + String.format("%.2f", grandTotal));

            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error generating turnover report.");
            }
        });

        card.add(filterRow, BorderLayout.NORTH);
        card.add(new JScrollPane(table), BorderLayout.CENTER);
        card.add(totalLabel, BorderLayout.SOUTH);

        outer.add(card, BorderLayout.CENTER);
        return outer;
    }

    private JPanel buildStockTab() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setOpaque(false);

        JPanel card = AppShell.createCard();
        card.setLayout(new BorderLayout(0, 18));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(232, 232, 232)),
                new EmptyBorder(20, 20, 20, 20)
        ));

        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        topRow.setOpaque(false);

        JButton refreshBtn = buildDarkButton("Refresh");
        JCheckBox lowStockOnly = new JCheckBox("Show low stock only");
        lowStockOnly.setOpaque(false);

        topRow.add(refreshBtn);
        topRow.add(lowStockOnly);

        String[] cols = {"Item ID", "Description", "Stock Qty", "Min Level", "Price £", "VAT %", "Stock Value £", "Status"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        JTable table = new JTable(model);
        styleTable(table);

        JLabel totalLabel = new JLabel(" ", SwingConstants.RIGHT);
        totalLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        totalLabel.setBorder(new EmptyBorder(4, 0, 4, 8));

        Runnable load = () -> {
            model.setRowCount(0);
            List<Product> products = lowStockOnly.isSelected()
                    ? ProductDB.getLowStockProducts()
                    : ProductDB.getAllProducts();

            double totalValue = 0;
            for (Product p : products) {
                double stockValue = p.getPrice() * p.getStockQuantity() * (1 + p.getVatRate() / 100);
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

        outer.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                load.run();
            }
        });

        card.add(topRow, BorderLayout.NORTH);
        card.add(new JScrollPane(table), BorderLayout.CENTER);
        card.add(totalLabel, BorderLayout.SOUTH);

        outer.add(card, BorderLayout.CENTER);
        return outer;
    }

    private JPanel buildDebtTab() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setOpaque(false);

        JPanel card = AppShell.createCard();
        card.setLayout(new BorderLayout(0, 18));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(232, 232, 232)),
                new EmptyBorder(20, 20, 20, 20)
        ));

        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        filterRow.setOpaque(false);

        JTextField fromField = buildTextField("2024-01-01", 12);
        JTextField toField = buildTextField("2099-12-31", 12);
        JButton runBtn = buildDarkButton("Generate");

        filterRow.add(new JLabel("From (YYYY-MM-DD):"));
        filterRow.add(fromField);
        filterRow.add(new JLabel("To (YYYY-MM-DD):"));
        filterRow.add(toField);
        filterRow.add(runBtn);

        String[] cols = {"Account No.", "Full Name", "Status", "Opening Debt £", "Payments Received £", "Current Balance £"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        JTable table = new JTable(model);
        styleTable(table);

        JLabel summaryLabel = new JLabel(" ", SwingConstants.RIGHT);
        summaryLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        summaryLabel.setBorder(new EmptyBorder(4, 0, 4, 8));

        runBtn.addActionListener(e -> {
            model.setRowCount(0);

            String paymentsSql = """
                SELECT customer_id, SUM(amount) AS total_paid
                FROM account_payments
                WHERE DATE(payment_date) BETWEEN ? AND ?
                GROUP BY customer_id
                """;

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

                double totalOpeningDebt = 0;
                double totalPayments = 0;
                double totalCurrentBalance = 0;

                for (Customer c : customers) {
                    double opening = openingDebt.getOrDefault(c.getCustomerId(), 0.0);
                    double paid = payments.getOrDefault(c.getCustomerId(), 0.0);
                    double balance = c.getCurrentBalance();

                    totalOpeningDebt += opening;
                    totalPayments += paid;
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
                JOptionPane.showMessageDialog(this, "Error generating debt report.");
            }
        });

        card.add(filterRow, BorderLayout.NORTH);
        card.add(new JScrollPane(table), BorderLayout.CENTER);
        card.add(summaryLabel, BorderLayout.SOUTH);

        outer.add(card, BorderLayout.CENTER);
        return outer;
    }

    private JTextField buildTextField(String text, int cols) {
        JTextField field = new JTextField(text, cols);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)),
                new EmptyBorder(6, 10, 6, 10)
        ));
        return field;
    }

    private void styleTable(JTable table) {
        table.setRowHeight(38);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFont(new Font("SansSerif", Font.PLAIN, 12));
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
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
}