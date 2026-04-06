package ui;

import database.ProductDB;
import domain.Product;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class StockPanel extends JPanel implements ThemeManager.ThemeListener {

    private final ScreenRouter router;

    private JPanel contentPanel;
    private JPanel tableCard;
    private JPanel topToolbar;
    private JPanel bottomActionBar;
    private JPanel footerPanel;
    private JLabel warningLabel;

    private JTable table;
    private JScrollPane scrollPane;

    private JButton orderStockBtn;
    private JButton editBtn;
    private JButton deleteBtn;
    private JButton restockBtn;
    private JButton refreshBtn;

    private JComboBox<String> filterCombo;
    private JComboBox<String> sortCombo;

    private DefaultTableModel tableModel;

    private final List<Product> allProducts = new ArrayList<>();
    private final List<Product> visibleProducts = new ArrayList<>();

    public StockPanel(ScreenRouter router) {
        this.router = router;

        setLayout(new BorderLayout());
        ThemeManager.register(this);

        AppShell shell = new AppShell(
                router,
                MainFrame.SCREEN_STOCK,
                "Stock Information",
                "Manage stock and inventory",
                buildContent()
        );

        add(shell, BorderLayout.CENTER);
        wireActions();
        loadTable();
        applyTheme();
    }

    private JPanel buildContent() {
        contentPanel = new JPanel(new BorderLayout(16, 16));
        contentPanel.setBorder(new EmptyBorder(6, 6, 6, 6));
        contentPanel.setOpaque(true);

        tableCard = AppShell.createCard();
        tableCard.setLayout(new BorderLayout(18, 18));
        tableCard.setBorder(new EmptyBorder(20, 20, 20, 20));

        buildToolbar();
        buildTable();
        buildFooter();
        buildBottomActionBar();

        tableCard.add(topToolbar, BorderLayout.NORTH);
        tableCard.add(scrollPane, BorderLayout.CENTER);

        JPanel southWrapper = new JPanel(new BorderLayout(12, 12));
        southWrapper.setOpaque(false);
        southWrapper.add(footerPanel, BorderLayout.NORTH);
        southWrapper.add(bottomActionBar, BorderLayout.SOUTH);

        tableCard.add(southWrapper, BorderLayout.SOUTH);

        contentPanel.add(tableCard, BorderLayout.CENTER);

        warningLabel = new JLabel(" ");
        warningLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        warningLabel.setBorder(new EmptyBorder(0, 4, 0, 0));
        contentPanel.add(warningLabel, BorderLayout.SOUTH);

        return contentPanel;
    }

    private void buildToolbar() {
        topToolbar = new JPanel(new BorderLayout());
        topToolbar.setOpaque(false);

        orderStockBtn = createPillButton("+  Order Stock", true);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.setOpaque(false);
        left.add(orderStockBtn);

        filterCombo = new JComboBox<>(new String[]{
                "All Stocks", "Good", "Low", "Restock"
        });

        sortCombo = new JComboBox<>(new String[]{
                "Sort by: Quantity", "Sort by: Stock ID", "Sort by: Price", "Sort by: Description"
        });

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        right.add(filterCombo);
        right.add(sortCombo);

        topToolbar.add(left, BorderLayout.WEST);
        topToolbar.add(right, BorderLayout.EAST);
    }

    private void buildTable() {
        String[] columns = {
                "Stock ID", "Description", "Stock Quantity", "Price", "Rate of Vat", "Status"
        };

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        configureTable(table);

        scrollPane = new JScrollPane(table);
        styleScrollPane(scrollPane);
    }

    private void buildFooter() {
        footerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        footerPanel.setOpaque(false);

        footerPanel.add(createPageChip("1", true));
        footerPanel.add(createPageChip("2", false));
        footerPanel.add(createPageChip("3", false));
        footerPanel.add(createPageChip("4", false));
        footerPanel.add(createPageChip("5", false));
        footerPanel.add(createPageChip("...", false));
        footerPanel.add(createPageChip("20", false));
    }

    private void buildBottomActionBar() {
        bottomActionBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        bottomActionBar.setOpaque(false);

        editBtn = createPillButton("Edit Product", false);
        deleteBtn = createPillButton("Remove Product", false);
        restockBtn = createPillButton("Restock", false);
        refreshBtn = createPillButton("Refresh", false);

        bottomActionBar.add(editBtn);
        bottomActionBar.add(deleteBtn);
        bottomActionBar.add(restockBtn);
        bottomActionBar.add(refreshBtn);
    }

    private void wireActions() {
        refreshBtn.addActionListener(e -> loadTable());
        orderStockBtn.addActionListener(e -> showAddDialog());

        editBtn.addActionListener(e -> {
            int selectedIndex = getSelectedVisibleIndex();
            if (selectedIndex == -1) {
                JOptionPane.showMessageDialog(this, "Please select a product to edit.");
                return;
            }
            showEditDialog(visibleProducts.get(selectedIndex));
        });

        deleteBtn.addActionListener(e -> {
            int selectedIndex = getSelectedVisibleIndex();
            if (selectedIndex == -1) {
                JOptionPane.showMessageDialog(this, "Please select a product to remove.");
                return;
            }

            Product selectedProduct = visibleProducts.get(selectedIndex);

            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Remove the selected product from stock?",
                    "Confirm removal",
                    JOptionPane.YES_NO_OPTION
            );

            if (confirm == JOptionPane.YES_OPTION) {
                ProductDB.deleteProduct(selectedProduct.getProductId());
                loadTable();
            }
        });

        restockBtn.addActionListener(e -> {
            int selectedIndex = getSelectedVisibleIndex();
            if (selectedIndex == -1) {
                JOptionPane.showMessageDialog(this, "Please select a product to restock.");
                return;
            }

            Product selectedProduct = visibleProducts.get(selectedIndex);

            String input = JOptionPane.showInputDialog(this, "Quantity to add:");
            if (input == null || input.trim().isEmpty()) {
                return;
            }

            try {
                int qty = Integer.parseInt(input.trim());
                if (qty <= 0) {
                    throw new NumberFormatException();
                }

                ProductDB.updateStock(selectedProduct.getProductId(), qty);
                loadTable();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter a valid positive quantity.");
            }
        });

        filterCombo.addActionListener(e -> refreshTableView());
        sortCombo.addActionListener(e -> refreshTableView());

        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                loadTable();
            }
        });
    }

    private int getSelectedVisibleIndex() {
        int viewRow = table.getSelectedRow();
        if (viewRow == -1) {
            return -1;
        }
        return table.convertRowIndexToModel(viewRow);
    }

    private void loadTable() {
        allProducts.clear();
        allProducts.addAll(ProductDB.getAllProducts());
        refreshTableView();

        int lowStockCount = ProductDB.getLowStockCount();
        if (lowStockCount > 0) {
            warningLabel.setText("Warning: " + lowStockCount + " product(s) are at or below minimum stock.");
        } else {
            warningLabel.setText("All products are above minimum stock.");
        }
    }

    private void refreshTableView() {
        tableModel.setRowCount(0);
        visibleProducts.clear();

        String selectedFilter = filterCombo != null ? (String) filterCombo.getSelectedItem() : "All Stocks";
        String selectedSort = sortCombo != null ? (String) sortCombo.getSelectedItem() : "Sort by: Quantity";

        List<Product> filtered = new ArrayList<>();
        for (Product product : allProducts) {
            String status = getStatusText(product);
            boolean include = "All Stocks".equals(selectedFilter)
                    || status.equalsIgnoreCase(selectedFilter);

            if (include) {
                filtered.add(product);
            }
        }

        if ("Sort by: Quantity".equals(selectedSort)) {
            filtered.sort(Comparator.comparingInt(Product::getStockQuantity));
        } else if ("Sort by: Stock ID".equals(selectedSort)) {
            filtered.sort(Comparator.comparingInt(Product::getProductId));
        } else if ("Sort by: Price".equals(selectedSort)) {
            filtered.sort(Comparator.comparingDouble(Product::getPrice));
        } else if ("Sort by: Description".equals(selectedSort)) {
            filtered.sort(Comparator.comparing(Product::getDescription, String.CASE_INSENSITIVE_ORDER));
        }

        visibleProducts.addAll(filtered);

        for (Product p : visibleProducts) {
            tableModel.addRow(new Object[]{
                    String.format("%03d", p.getProductId()),
                    p.getDescription(),
                    p.getStockQuantity(),
                    String.format("£%.2f", p.getPrice()),
                    String.format("%.1f%%", p.getVatRate()),
                    getStatusText(p)
            });
        }

        applyTheme();
    }

    private String getStatusText(Product product) {
        int stock = product.getStockQuantity();
        int min = product.getMinStockLevel();

        if (stock <= min) {
            return "Restock";
        } else if (stock <= min + 5) {
            return "Low";
        }
        return "Good";
    }

    private void showAddDialog() {
        Window window = SwingUtilities.getWindowAncestor(this);
        ProductFormDialog dialog = new ProductFormDialog(
                window,
                "Order / Add Stock",
                true,
                null
        );

        dialog.setVisible(true);

        if (!dialog.isConfirmed()) {
            return;
        }

        try {
            Product product = new Product(
                    0,
                    dialog.getItemId(),
                    dialog.getDescriptionText(),
                    dialog.getPackageType(),
                    dialog.getUnitsInPack(),
                    dialog.getPrice(),
                    dialog.getVatRate(),
                    dialog.getStockQuantity(),
                    dialog.getMinimumStock()
            );

            if (ProductDB.addProduct(product)) {
                loadTable();
            } else {
                JOptionPane.showMessageDialog(this, "Could not add product. Check that the Item ID is unique.");
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please check the numeric fields.");
        }
    }

    private void showEditDialog(Product product) {
        Product freshProduct = ProductDB.getById(product.getProductId());
        if (freshProduct == null) {
            JOptionPane.showMessageDialog(this, "Could not load the selected product.");
            return;
        }

        Window window = SwingUtilities.getWindowAncestor(this);
        ProductFormDialog dialog = new ProductFormDialog(
                window,
                "Edit Product",
                false,
                freshProduct
        );

        dialog.setVisible(true);

        if (!dialog.isConfirmed()) {
            return;
        }

        try {
            freshProduct.setDescription(dialog.getDescriptionText());
            freshProduct.setPackageType(dialog.getPackageType());
            freshProduct.setUnitsInPack(dialog.getUnitsInPack());
            freshProduct.setPrice(dialog.getPrice());
            freshProduct.setVatRate(dialog.getVatRate());
            freshProduct.setStockQuantity(dialog.getStockQuantity());
            freshProduct.setMinStockLevel(dialog.getMinimumStock());

            if (ProductDB.updateProduct(freshProduct)) {
                loadTable();
            } else {
                JOptionPane.showMessageDialog(this, "Could not update the product.");
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please check the numeric fields.");
        }
    }

    private void configureTable(JTable table) {
        table.setRowHeight(42);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFillsViewportHeight(true);
        table.setBorder(BorderFactory.createEmptyBorder());
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setDefaultEditor(Object.class, null);
        table.setFont(new Font("SansSerif", Font.PLAIN, 13));
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setResizingAllowed(false);
        header.setFont(new Font("SansSerif", Font.BOLD, 13));
        header.setBorder(BorderFactory.createEmptyBorder());
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 40));

        DefaultTableCellRenderer baseRenderer = new DefaultTableCellRenderer();
        baseRenderer.setBorder(new EmptyBorder(0, 10, 0, 10));
        baseRenderer.setHorizontalAlignment(SwingConstants.LEFT);

        for (int i = 0; i < table.getColumnCount() - 1; i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(baseRenderer);
        }

        table.getColumnModel().getColumn(5).setCellRenderer(new StatusCellRenderer());
    }

    private void styleScrollPane(JScrollPane sp) {
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setOpaque(true);
        sp.setBackground(ThemeManager.tableBackground());

        JViewport viewport = sp.getViewport();
        viewport.setBackground(ThemeManager.tableBackground());
        viewport.setBorder(null);
        viewport.setOpaque(true);
    }

    private JButton createPillButton(String text, boolean primary) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setFont(new Font("SansSerif", Font.BOLD, 13));
        button.setBorder(new EmptyBorder(10, 18, 10, 18));
        button.putClientProperty("primary", primary);
        stylePillButton(button, primary);
        return button;
    }

    private JButton createPageChip(String text, boolean active) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.setPreferredSize(new Dimension(26, 24));
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        if (active) {
            button.setBackground(new Color(77, 77, 77));
            button.setForeground(Color.WHITE);
        } else {
            button.setBackground(ThemeManager.isDark() ? new Color(55, 58, 66) : new Color(245, 245, 245));
            button.setForeground(ThemeManager.textSecondary());
        }

        return button;
    }

    private void stylePillButton(JButton button, boolean primary) {
        if (primary) {
            button.setBackground(ThemeManager.buttonDark());
            button.setForeground(ThemeManager.textLight());
            button.setBorder(new EmptyBorder(10, 18, 10, 18));
        } else {
            button.setBackground(ThemeManager.buttonLight());
            button.setForeground(ThemeManager.textPrimary());
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ThemeManager.borderColor()),
                    new EmptyBorder(9, 16, 9, 16)
            ));
        }
    }

    private void styleComboBox(JComboBox<String> comboBox) {
        comboBox.setFocusable(false);
        comboBox.setFont(new Font("SansSerif", Font.PLAIN, 13));
        comboBox.setPreferredSize(new Dimension(130, 30));
        comboBox.setBackground(ThemeManager.comboBackground());
        comboBox.setForeground(ThemeManager.comboForeground());
        comboBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.borderColor()),
                new EmptyBorder(4, 8, 4, 8)
        ));
    }

    private void applyTableTheme() {
        if (table == null) {
            return;
        }

        table.setBackground(ThemeManager.tableBackground());
        table.setForeground(ThemeManager.textPrimary());
        table.setSelectionBackground(ThemeManager.selectionBackground());
        table.setSelectionForeground(ThemeManager.textPrimary());

        JTableHeader header = table.getTableHeader();
        if (header != null) {
            header.setBackground(ThemeManager.tableBackground());
            header.setForeground(ThemeManager.textPrimary());
            header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ThemeManager.borderColor()));
            header.setOpaque(true);
        }

        table.repaint();
    }

    @Override
    public void applyTheme() {
        setBackground(ThemeManager.appBackground());

        if (contentPanel != null) {
            contentPanel.setBackground(ThemeManager.appBackground());
        }

        if (tableCard != null) {
            tableCard.setBackground(ThemeManager.panelBackground());
        }

        if (topToolbar != null) {
            topToolbar.setBackground(ThemeManager.panelBackground());
        }

        if (bottomActionBar != null) {
            bottomActionBar.setBackground(ThemeManager.panelBackground());
        }

        if (footerPanel != null) {
            footerPanel.setBackground(ThemeManager.panelBackground());
        }

        if (warningLabel != null) {
            warningLabel.setForeground(ProductDB.getLowStockCount() > 0
                    ? new Color(190, 76, 76)
                    : ThemeManager.textSecondary());
        }

        if (orderStockBtn != null) stylePillButton(orderStockBtn, true);
        if (editBtn != null) stylePillButton(editBtn, false);
        if (deleteBtn != null) stylePillButton(deleteBtn, false);
        if (restockBtn != null) stylePillButton(restockBtn, false);
        if (refreshBtn != null) stylePillButton(refreshBtn, false);

        if (filterCombo != null) styleComboBox(filterCombo);
        if (sortCombo != null) styleComboBox(sortCombo);

        if (scrollPane != null) styleScrollPane(scrollPane);
        applyTableTheme();

        repaint();
        revalidate();
    }

    private class StatusCellRenderer extends DefaultTableCellRenderer {
        public StatusCellRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
            setFont(new Font("SansSerif", Font.BOLD, 12));
            setBorder(new EmptyBorder(0, 10, 0, 10));
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column
        ) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            String status = value == null ? "" : value.toString();
            setForeground(Color.BLACK);

            if ("Restock".equalsIgnoreCase(status)) {
                setBackground(new Color(237, 106, 94));
            } else if ("Low".equalsIgnoreCase(status)) {
                setBackground(new Color(244, 213, 96));
            } else {
                setBackground(new Color(204, 227, 102));
            }

            if (isSelected) {
                setBorder(BorderFactory.createLineBorder(ThemeManager.isDark()
                        ? new Color(220, 220, 220)
                        : new Color(80, 80, 80), 1));
            } else {
                setBorder(new EmptyBorder(0, 10, 0, 10));
            }

            setText(status);
            return this;
        }
    }

    private static class RoundedDarkButtonUI extends javax.swing.plaf.basic.BasicButtonUI {
        @Override
        public void installUI(JComponent c) {
            super.installUI(c);
            JButton button = (JButton) c;
            button.setOpaque(false);
            button.setBorder(BorderFactory.createEmptyBorder(14, 40, 14, 40));
            button.setRolloverEnabled(true);
        }

        @Override
        public void paint(Graphics g, JComponent c) {
            JButton button = (JButton) c;
            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            ButtonModel model = button.getModel();

            Color backgroundColor;
            if (model.isPressed()) {
                backgroundColor = new Color(22, 24, 33);
            } else if (model.isRollover()) {
                backgroundColor = new Color(30, 32, 43);
            } else {
                backgroundColor = new Color(27, 29, 39);
            }

            int arc = c.getHeight();
            g2.setColor(backgroundColor);
            g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), arc, arc);

            FontMetrics fm = g2.getFontMetrics(button.getFont());
            String text = button.getText();
            int textX = (c.getWidth() - fm.stringWidth(text)) / 2;
            int textY = (c.getHeight() - fm.getHeight()) / 2 + fm.getAscent();

            g2.setFont(button.getFont());
            g2.setColor(button.getForeground());
            g2.drawString(text, textX, textY);

            g2.dispose();
        }
    }

    private static class ProductFormDialog extends JDialog {

        private final boolean addMode;
        private boolean confirmed = false;

        private JTextField itemIdField;
        private JTextField descField;
        private JTextField pkgField;
        private JTextField unitsField;
        private JTextField priceField;
        private JTextField vatField;
        private JTextField stockField;
        private JTextField minStockField;

        ProductFormDialog(Window owner, String title, boolean addMode, Product product) {
            super(owner, title, ModalityType.APPLICATION_MODAL);
            this.addMode = addMode;

            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            setResizable(false);
            setContentPane(buildUI(product));
            pack();
            setSize(520, 780);
            setLocationRelativeTo(owner);
        }

        private JPanel buildUI(Product product) {
            JPanel root = new JPanel(new GridBagLayout());
            root.setBackground(ThemeManager.appBackground());
            root.setBorder(new EmptyBorder(16, 16, 16, 16));

            JPanel card = new JPanel(new GridBagLayout());
            card.setBackground(ThemeManager.panelBackground());
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ThemeManager.borderColor()),
                    new EmptyBorder(20, 24, 20, 24)
            ));

            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.weightx = 1.0;
            c.fill = GridBagConstraints.HORIZONTAL;

            // Title
            JLabel titleLabel = new JLabel("Order / Add Stock");
            titleLabel.setFont(new Font("SansSerif", Font.BOLD, 28));
            titleLabel.setForeground(ThemeManager.textPrimary());
            c.insets = new Insets(0, 0, 10, 0);
            card.add(titleLabel, c);

            // Subtitle
            c.gridy++;
            JLabel subtitleLabel = new JLabel("Enter the product details below to add new stock.");
            subtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
            subtitleLabel.setForeground(ThemeManager.textSecondary());
            c.insets = new Insets(0, 0, 16, 0);
            card.add(subtitleLabel, c);

            // Fields
            itemIdField = createField();
            descField = createField();
            pkgField = createField();
            unitsField = createField();
            priceField = createField();
            vatField = createField();
            stockField = createField();
            minStockField = createField();

            if (product == null) {
                unitsField.setText("1");
                vatField.setText("0.00");
                stockField.setText("0");
                minStockField.setText("5");
            } else {
                itemIdField.setText(product.getItemId());
                descField.setText(product.getDescription());
                pkgField.setText(product.getPackageType());
                unitsField.setText(String.valueOf(product.getUnitsInPack()));
                priceField.setText(String.format("%.2f", product.getPrice()));
                vatField.setText(String.format("%.2f", product.getVatRate()));
                stockField.setText(String.valueOf(product.getStockQuantity()));
                minStockField.setText(String.valueOf(product.getMinStockLevel()));
                itemIdField.setEnabled(false);
            }

            addField(card, c, "Item ID", itemIdField);
            addField(card, c, "Description", descField);
            addField(card, c, "Package Type", pkgField);
            addField(card, c, "Units in Pack", unitsField);
            addField(card, c, "Price £", priceField);
            addField(card, c, "VAT %", vatField);
            addField(card, c, "Stock Quantity", stockField);
            addField(card, c, "Minimum Stock", minStockField);

            // Confirm Button (Styled like Add Customer)
            JPanel buttonWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            buttonWrap.setOpaque(false);

            JButton confirmButton = new JButton("Confirm");
            confirmButton.setFont(new Font("SansSerif", Font.BOLD, 18));
            confirmButton.setForeground(Color.WHITE);
            confirmButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            confirmButton.setFocusPainted(false);
            confirmButton.setContentAreaFilled(false);
            confirmButton.setBorder(BorderFactory.createEmptyBorder(14, 40, 14, 40));
            confirmButton.setUI(new RoundedDarkButtonUI());

            confirmButton.addActionListener(e -> onConfirm());

            buttonWrap.add(confirmButton);

            c.gridy++;
            c.insets = new Insets(20, 0, 0, 0);
            card.add(buttonWrap, c);

            root.add(card);
            getRootPane().setDefaultButton(confirmButton);

            return root;
        }

        private void addField(JPanel panel, GridBagConstraints c, String labelText, JTextField field) {
            JLabel label = new JLabel(labelText);
            label.setFont(new Font("SansSerif", Font.BOLD, 13));
            label.setForeground(ThemeManager.textSecondary());

            JPanel block = new JPanel(new BorderLayout(0, 4));
            block.setOpaque(false);
            block.add(label, BorderLayout.NORTH);
            block.add(field, BorderLayout.CENTER);

            c.gridy++;
            c.insets = new Insets(0, 0, 10, 0);
            panel.add(block, c);
        }

        private JTextField createField() {
            JTextField field = new JTextField();
            field.setFont(new Font("SansSerif", Font.PLAIN, 15));
            field.setPreferredSize(new Dimension(0, 40));
            field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ThemeManager.borderColor()),
                    new EmptyBorder(8, 10, 8, 10)
            ));
            return field;
        }

        private void onConfirm() {
            if (itemIdField.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Item ID is required.");
                return;
            }

            if (descField.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Description is required.");
                return;
            }

            try {
                Integer.parseInt(unitsField.getText().trim());
                Double.parseDouble(priceField.getText().trim());
                Double.parseDouble(vatField.getText().trim());
                Integer.parseInt(stockField.getText().trim());
                Integer.parseInt(minStockField.getText().trim());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please check numeric fields.");
                return;
            }

            confirmed = true;
            dispose();
        }

        public boolean isConfirmed() { return confirmed; }
        public String getItemId() { return itemIdField.getText().trim(); }
        public String getDescriptionText() { return descField.getText().trim(); }
        public String getPackageType() { return pkgField.getText().trim(); }
        public int getUnitsInPack() { return Integer.parseInt(unitsField.getText().trim()); }
        public double getPrice() { return Double.parseDouble(priceField.getText().trim()); }
        public double getVatRate() { return Double.parseDouble(vatField.getText().trim()); }
        public int getStockQuantity() { return Integer.parseInt(stockField.getText().trim()); }
        public int getMinimumStock() { return Integer.parseInt(minStockField.getText().trim()); }
    }
}