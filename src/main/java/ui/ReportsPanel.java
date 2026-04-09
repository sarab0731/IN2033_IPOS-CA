
package ui;

import app.Session;
import database.CustomerDB;
import database.DatabaseManager;
import database.ProductDB;
import database.SaleDB;
import domain.Customer;
import domain.Product;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;

public class ReportsPanel extends JPanel implements ThemeManager.ThemeListener {

    private final ScreenRouter router;

    private JPanel contentPanel;
    private JPanel topTabBar;
    private JPanel controlsPanel;
    private JPanel tableCard;

    private JTable table;
    private JScrollPane scrollPane;
    private DefaultTableModel tableModel;

    private JTextField fromDateField;
    private JTextField toDateField;
    private JButton generateBtn;

    private JLabel fromLabel;
    private JLabel toLabel;
    private String activeReport = "TURNOVER";

    public ReportsPanel(ScreenRouter router) {
        this.router = router;

        setLayout(new BorderLayout());
        ThemeManager.register(this);

        AppShell shell = new AppShell(
                router,
                MainFrame.SCREEN_REPORTS,
                "Reports",
                "Generate sales, stock, and debt reports",
                buildContent()
        );

        add(shell, BorderLayout.CENTER);
        wireActions();
        applyTheme();
    }

    private JPanel buildContent() {
        contentPanel = new JPanel(new BorderLayout(20, 20));
        contentPanel.setBorder(new EmptyBorder(8, 8, 8, 8));

        topTabBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 18, 0));
        topTabBar.setOpaque(false);

        JButton turnoverTab = new JButton("Turnover");
        JButton stockAvailabilityTab = new JButton("Stock Availability");
        JButton customerDebtTab = new JButton("Customer Debt");
        JButton exportPdfBtn = new JButton("Export PDF");

        turnoverTab.setFocusable(false);
        stockAvailabilityTab.setFocusable(false);
        customerDebtTab.setFocusable(false);
        exportPdfBtn.setFont(new Font("SansSerif", Font.BOLD, 13));


        turnoverTab.addActionListener(e -> { activeReport = "TURNOVER"; generateReport(); });
        stockAvailabilityTab.addActionListener(e -> { activeReport = "STOCK"; generateReport(); });
        customerDebtTab.addActionListener(e -> { activeReport = "DEBT"; generateReport(); });
        exportPdfBtn.addActionListener(e -> exportReportToPdf());

        topTabBar.add(turnoverTab);
        topTabBar.add(stockAvailabilityTab);
        topTabBar.add(customerDebtTab);

        controlsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        controlsPanel.setOpaque(false);

        fromLabel = new JLabel("From (YYYY-MM-DD):");
        toLabel = new JLabel("To (YYYY-MM-DD):");

        fromDateField = new JTextField("2024-01-01", 12);
        toDateField = new JTextField("2099-12-31", 12);
        generateBtn = new JButton("Generate");

        controlsPanel.add(fromLabel);
        controlsPanel.add(fromDateField);
        controlsPanel.add(toLabel);
        controlsPanel.add(toDateField);
        controlsPanel.add(generateBtn);
        controlsPanel.add(exportPdfBtn);

        tableCard = AppShell.createCard();
        tableCard.setLayout(new BorderLayout());

        tableModel = new DefaultTableModel(new String[]{"Item ID", "Description", "Qty Sold", "Revenue £"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        table = new JTable(tableModel);
        configureTable(table);

        scrollPane = new JScrollPane(table);
        styleScrollPane(scrollPane);

        tableCard.add(scrollPane, BorderLayout.CENTER);

        JPanel wrapper = new JPanel(new BorderLayout(20, 20));
        wrapper.setOpaque(false);
        wrapper.add(controlsPanel, BorderLayout.NORTH);
        wrapper.add(tableCard, BorderLayout.CENTER);

        contentPanel.add(topTabBar, BorderLayout.NORTH);
        contentPanel.add(wrapper, BorderLayout.CENTER);

        return contentPanel;
    }

    private void wireActions() {
        generateBtn.addActionListener(e -> generateReport());
    }

    private void generateReport() {
        if (!Session.isManagerOrAdmin()) {
            JOptionPane.showMessageDialog(this, "Only Managers and Admins can generate reports.");
            return;
        }
        String from = fromDateField.getText().trim();
        String to = toDateField.getText().trim();

        try {
            switch (activeReport) {
                case "TURNOVER" -> generateTurnoverReport(from, to);
                case "STOCK" -> generateStockReport();
                case "DEBT" -> generateDebtReport();
                default -> generateTurnoverReport(from, to);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not generate the report.\n" + ex.getMessage());
        }
    }

    private void exportReportToPdf() {
        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No data to export.");
            return;
        }

        java.io.File dest = PdfGenerator.chooseSaveFile(this, "report-" + activeReport.toLowerCase() + ".pdf");
        if (dest == null) return;

        try {
            com.itextpdf.kernel.pdf.PdfDocument pdf =
                    new com.itextpdf.kernel.pdf.PdfDocument(new com.itextpdf.kernel.pdf.PdfWriter(dest));
            com.itextpdf.layout.Document doc =
                    new com.itextpdf.layout.Document(pdf, com.itextpdf.kernel.geom.PageSize.A4);
            doc.setMargins(40, 40, 40, 40);

            com.itextpdf.kernel.font.PdfFont bold =
                    com.itextpdf.kernel.font.PdfFontFactory.createFont(
                            com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD);
            com.itextpdf.kernel.font.PdfFont regular =
                    com.itextpdf.kernel.font.PdfFontFactory.createFont(
                            com.itextpdf.io.font.constants.StandardFonts.HELVETICA);

            doc.add(PdfGenerator.para("IPOS-CA Report — " + activeReport, bold, 14));
            doc.add(PdfGenerator.para("Generated: " + java.time.LocalDate.now(), regular, 10));
            doc.add(PdfGenerator.spacer(12));

            // Build column widths
            int cols = tableModel.getColumnCount();
            float[] widths = new float[cols];
            for (int i = 0; i < cols; i++) widths[i] = 500f / cols;

            com.itextpdf.layout.element.Table table =
                    new com.itextpdf.layout.element.Table(widths);
            table.setWidth(com.itextpdf.layout.properties.UnitValue.createPercentValue(100));

            for (int i = 0; i < cols; i++) {
                PdfGenerator.addHeaderCell(table, tableModel.getColumnName(i), bold);
            }

            for (int r = 0; r < tableModel.getRowCount(); r++) {
                for (int c = 0; c < cols; c++) {
                    Object val = tableModel.getValueAt(r, c);
                    table.addCell(PdfGenerator.dataCell(val != null ? val.toString() : "", regular));
                }
            }

            doc.add(table);
            doc.close();
            JOptionPane.showMessageDialog(this, "Report saved:\n" + dest.getAbsolutePath());

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to export: " + ex.getMessage());
        }
    }
    private void generateTurnoverReport(String from, String to) {
        tableModel.setColumnIdentifiers(new String[]{"Item ID", "Description", "Qty Sold", "Revenue £"});
        tableModel.setRowCount(0);

        List<Map<String, Object>> rows = SaleDB.getSalesByProduct(from, to);
        for (Map<String, Object> row : rows) {
            tableModel.addRow(new Object[]{
                    row.get("item_id"),
                    row.get("description"),
                    row.get("qty_sold"),
                    String.format("%.2f", (Double) row.get("revenue"))
            });
        }

        double turnover = SaleDB.getTurnover(from, to);
        if (rows.isEmpty()) {
            tableModel.addRow(new Object[]{"-", "No sales in range", 0, "0.00"});
        }
        tableModel.addRow(new Object[]{"", "TOTAL TURNOVER", "", String.format("%.2f", turnover)});
    }

    private void generateStockReport() {
        tableModel.setColumnIdentifiers(new String[]{"Item ID", "Description", "Stock Qty", "Min Stock", "Status"});
        tableModel.setRowCount(0);

        for (Product p : ProductDB.getAllProducts()) {
            tableModel.addRow(new Object[]{
                    p.getItemId(),
                    p.getDescription(),
                    p.getStockQuantity(),
                    p.getMinStockLevel(),
                    p.isLowStock() ? "LOW STOCK" : "OK"
            });
        }
    }

    private void generateDebtReport() throws Exception {
        tableModel.setColumnIdentifiers(new String[]{"Customer", "Account No.", "Balance £", "Status"});
        tableModel.setRowCount(0);

        for (Customer c : CustomerDB.getAllActiveCustomers()) {
            if (c.getCurrentBalance() > 0) {
                tableModel.addRow(new Object[]{
                        c.getFullName(),
                        c.getAccountNumber(),
                        String.format("%.2f", c.getCurrentBalance()),
                        c.getAccountStatus()
                });
            }
        }

        if (tableModel.getRowCount() == 0) {
            tableModel.addRow(new Object[]{"No outstanding customer debt", "", "0.00", ""});
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

    private void styleField(JTextField field) {
        field.setBackground(ThemeManager.fieldBackground());
        field.setForeground(ThemeManager.fieldForeground());
        field.setCaretColor(ThemeManager.fieldForeground());
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.borderColor()),
                new EmptyBorder(8, 10, 8, 10)
        ));
    }

    @Override
    public void applyTheme() {
        setBackground(ThemeManager.appBackground());

        if (contentPanel != null) contentPanel.setBackground(ThemeManager.appBackground());
        if (topTabBar != null) topTabBar.setBackground(ThemeManager.appBackground());
        if (controlsPanel != null) controlsPanel.setBackground(ThemeManager.appBackground());
        if (tableCard != null) tableCard.setBackground(ThemeManager.panelBackground());

        if (fromLabel != null) fromLabel.setForeground(ThemeManager.textPrimary());
        if (toLabel != null) toLabel.setForeground(ThemeManager.textPrimary());

        if (fromDateField != null) styleField(fromDateField);
        if (toDateField != null) styleField(toDateField);
        if (generateBtn != null) stylePrimaryButton(generateBtn);

        if (table != null) applyTableTheme(table);
        if (scrollPane != null) styleScrollPane(scrollPane);

        repaint();
        revalidate();
    }
}
