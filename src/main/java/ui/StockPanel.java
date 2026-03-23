package ui;

import database.ProductDB;
import domain.Product;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class StockPanel extends JPanel {

    private DefaultTableModel tableModel;
    private JTable table;
    private JLabel lowStockWarning;

    public StockPanel(ScreenRouter router) {
        setLayout(new BorderLayout());

        // --- HEADER ---
        JPanel header = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Stock Management", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 28));

        JButton backBtn = new JButton("Back");
        backBtn.addActionListener(e -> router.goTo(MainFrame.SCREEN_DASHBOARD));

        header.add(backBtn, BorderLayout.WEST);
        header.add(title, BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        // --- LOW STOCK WARNING ---
        lowStockWarning = new JLabel(" ", SwingConstants.CENTER);
        lowStockWarning.setFont(new Font("SansSerif", Font.BOLD, 13));
        lowStockWarning.setForeground(Color.RED);
        add(lowStockWarning, BorderLayout.SOUTH);

        // --- TABLE ---
        String[] columns = {"ID", "Item ID", "Description", "Package", "Units/Pack",
                "Price £", "VAT %", "Stock", "Min Stock", "Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        add(new JScrollPane(table), BorderLayout.CENTER);

        // --- BUTTONS ---
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 8));

        JButton addBtn      = new JButton("Add Product");
        JButton editBtn     = new JButton("Edit Product");
        JButton deleteBtn   = new JButton("Remove Product");
        JButton restockBtn  = new JButton("Update Stock");
        JButton refreshBtn  = new JButton("Refresh");

        buttons.add(addBtn);
        buttons.add(editBtn);
        buttons.add(deleteBtn);
        buttons.add(restockBtn);
        buttons.add(refreshBtn);

        add(buttons, BorderLayout.SOUTH);

        // re-add warning above buttons
        JPanel south = new JPanel(new BorderLayout());
        south.add(lowStockWarning, BorderLayout.NORTH);
        south.add(buttons, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);

        // --- ACTIONS ---
        refreshBtn.addActionListener(e -> loadTable());

        addBtn.addActionListener(e -> showAddDialog());

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
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to remove this product?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                int productId = (int) tableModel.getValueAt(row, 0);
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
            if (input == null || input.trim().isEmpty()) return;
            try {
                int qty = Integer.parseInt(input.trim());
                int productId = (int) tableModel.getValueAt(row, 0);
                ProductDB.updateStock(productId, qty);
                loadTable();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter a valid number.");
            }
        });

        // load on show
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                loadTable();
            }
        });
    }

    private void loadTable() {
        tableModel.setRowCount(0);
        List<Product> products = ProductDB.getAllProducts();

        for (Product p : products) {
            String status = p.isLowStock() ? "LOW STOCK" : "OK";
            tableModel.addRow(new Object[]{
                    p.getProductId(),
                    p.getItemId(),
                    p.getDescription(),
                    p.getPackageType(),
                    p.getUnitsInPack(),
                    String.format("%.2f", p.getPrice()),
                    String.format("%.2f", p.getVatRate()),
                    p.getStockQuantity(),
                    p.getMinStockLevel(),
                    status
            });
        }

        // low stock warning
        List<Product> lowStock = ProductDB.getLowStockProducts();
        if (!lowStock.isEmpty()) {
            lowStockWarning.setText("WARNING: " + lowStock.size() + " product(s) are below minimum stock level.");
        } else {
            lowStockWarning.setText(" ");
        }
    }

    private void showAddDialog() {
        JTextField itemIdField   = new JTextField();
        JTextField descField     = new JTextField();
        JTextField pkgField      = new JTextField();
        JTextField unitsField    = new JTextField("1");
        JTextField priceField    = new JTextField();
        JTextField vatField      = new JTextField("0.00");
        JTextField stockField    = new JTextField("0");
        JTextField minStockField = new JTextField("0");

        Object[] fields = {
                "Item ID:",        itemIdField,
                "Description:",    descField,
                "Package Type:",   pkgField,
                "Units in Pack:",  unitsField,
                "Price £:",        priceField,
                "VAT %:",          vatField,
                "Stock Qty:",      stockField,
                "Min Stock Level:",minStockField
        };

        int result = JOptionPane.showConfirmDialog(this, fields, "Add Product", JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) return;

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
        int productId   = (int) tableModel.getValueAt(row, 0);
        JTextField descField     = new JTextField((String) tableModel.getValueAt(row, 2));
        JTextField pkgField      = new JTextField((String) tableModel.getValueAt(row, 3));
        JTextField unitsField    = new JTextField(tableModel.getValueAt(row, 4).toString());
        JTextField priceField    = new JTextField(tableModel.getValueAt(row, 5).toString());
        JTextField vatField      = new JTextField(tableModel.getValueAt(row, 6).toString());
        JTextField stockField    = new JTextField(tableModel.getValueAt(row, 7).toString());
        JTextField minStockField = new JTextField(tableModel.getValueAt(row, 8).toString());

        Object[] fields = {
                "Description:",    descField,
                "Package Type:",   pkgField,
                "Units in Pack:",  unitsField,
                "Price £:",        priceField,
                "VAT %:",          vatField,
                "Stock Qty:",      stockField,
                "Min Stock Level:",minStockField
        };

        int result = JOptionPane.showConfirmDialog(this, fields, "Edit Product", JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) return;

        try {
            Product p = new Product(
                    productId,
                    (String) tableModel.getValueAt(row, 1),
                    descField.getText().trim(),
                    pkgField.getText().trim(),
                    Integer.parseInt(unitsField.getText().trim()),
                    Double.parseDouble(priceField.getText().trim()),
                    Double.parseDouble(vatField.getText().trim()),
                    Integer.parseInt(stockField.getText().trim()),
                    Integer.parseInt(minStockField.getText().trim())
            );
            ProductDB.updateProduct(p);
            loadTable();

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please check numeric fields.");
        }
    }
}