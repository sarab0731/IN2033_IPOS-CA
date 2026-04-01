package ui;

import database.ProductDB;
import domain.Product;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class StockPanel extends JPanel {

    private DefaultTableModel tableModel;
    private JTable table;
    private JLabel lowStockWarning;

    private JComboBox<String> filterCombo;
    private JComboBox<String> sortCombo;

    private List<Product> currentProducts = new ArrayList<>();

    public StockPanel(ScreenRouter router) {
        setLayout(new BorderLayout());
        setBackground(new Color(242, 242, 242));

        JPanel stockContent = buildStockContent(router);

        AppShell shell = new AppShell(
                router,
                MainFrame.SCREEN_STOCK,
                "Stock Information",
                "Manage stock and inventory",
                stockContent
        );

        add(shell, BorderLayout.CENTER);

        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                loadTable();
            }
        });
    }

    private JPanel buildStockContent(ScreenRouter router) {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setOpaque(false);

        JPanel card = AppShell.createCard();
        card.setLayout(new BorderLayout(0, 18));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(232, 232, 232)),
                new EmptyBorder(20, 20, 20, 20)
        ));

        card.add(buildTopBar(router), BorderLayout.NORTH);
        card.add(buildTableSection(), BorderLayout.CENTER);
        card.add(buildFooterSection(), BorderLayout.SOUTH);

        outer.add(card, BorderLayout.CENTER);
        return outer;
    }

    private JPanel buildTopBar(ScreenRouter router) {
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JButton orderStockBtn = new JButton("+  Order Stock");
        orderStockBtn.setFocusPainted(false);
        orderStockBtn.setBorderPainted(false);
        orderStockBtn.setContentAreaFilled(true);
        orderStockBtn.setOpaque(true);
        orderStockBtn.setBackground(new Color(30, 32, 38));
        orderStockBtn.setForeground(Color.WHITE);
        orderStockBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        orderStockBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        orderStockBtn.setPreferredSize(new Dimension(120, 30));
        orderStockBtn.addActionListener(e -> showAddDialog());

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.setOpaque(false);
        left.add(orderStockBtn);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);

        filterCombo = new JComboBox<>(new String[]{"All Stocks", "Low Stock", "Good Stock"});
        sortCombo = new JComboBox<>(new String[]{
                "Sort by: Quantity",
                "Sort by: Item ID",
                "Sort by: Description",
                "Sort by: Price"
        });

        styleCombo(filterCombo);
        styleCombo(sortCombo);

        filterCombo.addActionListener(e -> applyFiltersAndSorting());
        sortCombo.addActionListener(e -> applyFiltersAndSorting());

        right.add(filterCombo);
        right.add(sortCombo);

        top.add(left, BorderLayout.WEST);
        top.add(right, BorderLayout.EAST);

        return top;
    }

    private JPanel buildTableSection() {
        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);

        String[] columns = {
                "Stock ID", "Description", "Package", "Units/Pack",
                "Stock Quantity", "Price", "Rate of Vat", "Status"
        };

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(42);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFont(new Font("SansSerif", Font.PLAIN, 12));
        table.setForeground(new Color(35, 35, 35));
        table.setSelectionBackground(new Color(235, 235, 235));
        table.setSelectionForeground(new Color(35, 35, 35));

        JTableHeaderStyle(table);

        table.getColumnModel().getColumn(7).setCellRenderer(new StatusCellRenderer());

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);

        center.add(scrollPane, BorderLayout.CENTER);

        return center;
    }

    private JPanel buildFooterSection() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);

        lowStockWarning = new JLabel(" ");
        lowStockWarning.setFont(new Font("SansSerif", Font.BOLD, 12));
        lowStockWarning.setForeground(new Color(210, 70, 70));

        JPanel warningWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        warningWrap.setOpaque(false);
        warningWrap.add(lowStockWarning);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        actions.setOpaque(false);

        JButton editBtn = buildActionButton("Edit Product");
        JButton deleteBtn = buildActionButton("Remove Product");
        JButton restockBtn = buildActionButton("Update Stock");
        JButton refreshBtn = buildActionButton("Refresh");

        editBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Please select a product to edit.");
                return;
            }
            showEditDialog(row);
        });

        deleteBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Please select a product to remove.");
                return;
            }

            int productId = Integer.parseInt(tableModel.getValueAt(row, 0).toString());

            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Are you sure you want to remove this product?",
                    "Confirm",
                    JOptionPane.YES_NO_OPTION
            );

            if (confirm == JOptionPane.YES_OPTION) {
                ProductDB.deleteProduct(productId);
                loadTable();
            }
        });

        restockBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Please select a product to restock.");
                return;
            }

            String input = JOptionPane.showInputDialog(this, "Quantity to add:");
            if (input == null || input.trim().isEmpty()) {
                return;
            }

            try {
                int qty = Integer.parseInt(input.trim());
                int productId = Integer.parseInt(tableModel.getValueAt(row, 0).toString());
                ProductDB.updateStock(productId, qty);
                loadTable();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter a valid number.");
            }
        });

        refreshBtn.addActionListener(e -> loadTable());

        actions.add(editBtn);
        actions.add(deleteBtn);
        actions.add(restockBtn);
        actions.add(refreshBtn);

        footer.add(warningWrap, BorderLayout.NORTH);
        footer.add(actions, BorderLayout.CENTER);

        return footer;
    }

    private JButton buildActionButton(String text) {
        JButton btn = new JButton(text);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(true);
        btn.setOpaque(true);
        btn.setBackground(new Color(245, 245, 245));
        btn.setForeground(new Color(45, 45, 45));
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)),
                new EmptyBorder(8, 14, 8, 14)
        ));
        return btn;
    }

    private void styleCombo(JComboBox<String> combo) {
        combo.setFont(new Font("SansSerif", Font.PLAIN, 12));
        combo.setBackground(Color.WHITE);
        combo.setForeground(new Color(80, 80, 80));
        combo.setFocusable(false);
        combo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)),
                new EmptyBorder(4, 8, 4, 8)
        ));
    }

    private void JTableHeaderStyle(JTable table) {
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.getTableHeader().setBackground(Color.WHITE);
        table.getTableHeader().setForeground(new Color(25, 25, 25));
        table.getTableHeader().setBorder(BorderFactory.createEmptyBorder());
        table.getTableHeader().setReorderingAllowed(false);
    }

    private void loadTable() {
        currentProducts = ProductDB.getAllProducts();
        applyFiltersAndSorting();

        List<Product> lowStock = ProductDB.getLowStockProducts();
        if (!lowStock.isEmpty()) {
            lowStockWarning.setText("Warning: " + lowStock.size() + " product(s) are below minimum stock level.");
        } else {
            lowStockWarning.setText(" ");
        }
    }

    private void applyFiltersAndSorting() {
        if (tableModel == null) {
            return;
        }

        tableModel.setRowCount(0);

        List<Product> displayList = new ArrayList<>(currentProducts);

        String filter = filterCombo != null ? (String) filterCombo.getSelectedItem() : "All Stocks";
        String sort = sortCombo != null ? (String) sortCombo.getSelectedItem() : "Sort by: Quantity";

        if ("Low Stock".equals(filter)) {
            displayList.removeIf(p -> !p.isLowStock());
        } else if ("Good Stock".equals(filter)) {
            displayList.removeIf(Product::isLowStock);
        }

        if ("Sort by: Quantity".equals(sort)) {
            displayList.sort(Comparator.comparingInt(Product::getStockQuantity));
        } else if ("Sort by: Item ID".equals(sort)) {
            displayList.sort(Comparator.comparing(Product::getItemId, String.CASE_INSENSITIVE_ORDER));
        } else if ("Sort by: Description".equals(sort)) {
            displayList.sort(Comparator.comparing(Product::getDescription, String.CASE_INSENSITIVE_ORDER));
        } else if ("Sort by: Price".equals(sort)) {
            displayList.sort(Comparator.comparingDouble(Product::getPrice));
        }

        for (Product p : displayList) {
            tableModel.addRow(new Object[]{
                    p.getProductId(),
                    p.getDescription(),
                    p.getPackageType(),
                    p.getUnitsInPack(),
                    p.getStockQuantity(),
                    String.format("£%.2f", p.getPrice()),
                    String.format("%.1f%%", p.getVatRate()),
                    getStatusText(p)
            });
        }
    }

    private String getStatusText(Product p) {
        if (p.getStockQuantity() <= p.getMinStockLevel()) {
            return "Restock";
        } else if (p.getStockQuantity() <= p.getMinStockLevel() + 5) {
            return "Low";
        } else {
            return "Good";
        }
    }

    private void showAddDialog() {
        JTextField itemIdField = new JTextField();
        JTextField descField = new JTextField();
        JTextField pkgField = new JTextField();
        JTextField unitsField = new JTextField("1");
        JTextField priceField = new JTextField();
        JTextField vatField = new JTextField("0.00");
        JTextField stockField = new JTextField("0");
        JTextField minStockField = new JTextField("0");

        Object[] fields = {
                "Item ID:", itemIdField,
                "Description:", descField,
                "Package Type:", pkgField,
                "Units in Pack:", unitsField,
                "Price £:", priceField,
                "VAT %:", vatField,
                "Stock Qty:", stockField,
                "Min Stock Level:", minStockField
        };

        int result = JOptionPane.showConfirmDialog(this, fields, "Add Product", JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            Product p = new Product(
                    0,
                    itemIdField.getText().trim(),
                    descField.getText().trim(),
                    pkgField.getText().trim(),
                    Integer.parseInt(unitsField.getText().trim()),
                    Double.parseDouble(priceField.getText().trim()),
                    Double.parseDouble(vatField.getText().trim()),
                    Integer.parseInt(stockField.getText().trim()),
                    Integer.parseInt(minStockField.getText().trim())
            );

            if (ProductDB.addProduct(p)) {
                loadTable();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to add product. Item ID may already exist.");
            }

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please check numeric fields.");
        }
    }

    private void showEditDialog(int row) {
        int productId = Integer.parseInt(tableModel.getValueAt(row, 0).toString());
        Product existing = ProductDB.getById(productId);

        if (existing == null) {
            JOptionPane.showMessageDialog(this, "Could not load the selected product.");
            return;
        }

        JTextField descField = new JTextField(existing.getDescription());
        JTextField pkgField = new JTextField(existing.getPackageType());
        JTextField unitsField = new JTextField(String.valueOf(existing.getUnitsInPack()));
        JTextField priceField = new JTextField(String.valueOf(existing.getPrice()));
        JTextField vatField = new JTextField(String.valueOf(existing.getVatRate()));
        JTextField stockField = new JTextField(String.valueOf(existing.getStockQuantity()));
        JTextField minStockField = new JTextField(String.valueOf(existing.getMinStockLevel()));

        Object[] fields = {
                "Description:", descField,
                "Package Type:", pkgField,
                "Units in Pack:", unitsField,
                "Price £:", priceField,
                "VAT %:", vatField,
                "Stock Qty:", stockField,
                "Min Stock Level:", minStockField
        };

        int result = JOptionPane.showConfirmDialog(this, fields, "Edit Product", JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            Product updated = new Product(
                    existing.getProductId(),
                    existing.getItemId(),
                    descField.getText().trim(),
                    pkgField.getText().trim(),
                    Integer.parseInt(unitsField.getText().trim()),
                    Double.parseDouble(priceField.getText().trim()),
                    Double.parseDouble(vatField.getText().trim()),
                    Integer.parseInt(stockField.getText().trim()),
                    Integer.parseInt(minStockField.getText().trim())
            );

            if (ProductDB.updateProduct(updated)) {
                loadTable();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to update product.");
            }

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please check numeric fields.");
        }
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

                switch (status) {
                    case "Restock" -> label.setBackground(new Color(234, 105, 90));
                    case "Low" -> label.setBackground(new Color(240, 205, 90));
                    case "Good" -> label.setBackground(new Color(201, 224, 98));
                    default -> label.setBackground(Color.WHITE);
                }
            }

            return label;
        }
    }
}