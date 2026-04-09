package ui;

import app.Session;
import database.ProductDB;
import database.RestockOrderDB;
import domain.Product;
import domain.RestockOrder;
import domain.RestockOrderItem;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * "Stock Management" screen — 5 tabs in workflow order:
 *   Stock → Low stock → Place order → Order history → Deliveries
 */
public class StockPanel extends JPanel implements ThemeManager.ThemeListener {

    private final ScreenRouter router;

    // ── Tab bar
    private JButton stockTabBtn;
    private JButton lowStockTabBtn;
    private JButton placeOrderTabBtn;
    private JButton historyTabBtn;
    private JButton deliveriesTabBtn;
    private String activeTab = "stock";

    private JPanel tabContent;
    private CardLayout tabCards;

    // ── Stock tab
    private JPanel tableCard;
    private JPanel topToolbar;
    private JPanel bottomActionBar;
    private JPanel footerPanel;
    private JLabel warningLabel;
    private JTable stockTable;
    private JScrollPane stockScrollPane;
    private JButton orderStockBtn;
    private JButton editBtn;
    private JButton deleteBtn;
    private JButton restockBtn;
    private JButton refreshBtn;
    private JComboBox<String> filterCombo;
    private JComboBox<String> sortCombo;
    private JTextField searchField;
    private DefaultTableModel stockModel;
    private final List<Product> allProducts = new ArrayList<>();
    private final List<Product> visibleProducts = new ArrayList<>();

    // ── Low stock tab
    private JTable lowStockTable;
    private DefaultTableModel lowStockModel;
    private JScrollPane lowStockScroll;

    // ── Place order tab
    private JTable productsTable;
    private JTable summaryTable;
    private JScrollPane productsScrollPane;
    private JScrollPane summaryScrollPane;
    private JButton addToOrderBtn;
    private JButton removeItemBtn;
    private JButton clearOrderBtn;
    private JButton placeOrderBtn;
    private JLabel merchantIdValue;
    private JLabel totalLabel;
    private JLabel activeOrdersValue;
    private JLabel outstandingValue;
    private DefaultTableModel productsModel;
    private DefaultTableModel summaryModel;
    private final Map<Product, Integer> cart = new LinkedHashMap<>();

    // ── Order history tab
    private JTable historyTable;
    private DefaultTableModel historyModel;
    private JScrollPane historyScroll;

    // ── Deliveries tab
    private JTable deliveriesTable;
    private DefaultTableModel deliveriesModel;
    private JScrollPane deliveriesScroll;

    public StockPanel(ScreenRouter router) {
        this.router = router;
        setLayout(new BorderLayout());
        ThemeManager.register(this);

        AppShell shell = new AppShell(
                router,
                MainFrame.SCREEN_STOCK,
                "Stock Management",
                "Stock management and supplier orders",
                buildContent()
        );

        add(shell, BorderLayout.CENTER);
        wireStockActions();
        loadStockTable();
        applyTheme();
    }

    // ─────────────────────────────────────────────────
    // Main layout
    // ─────────────────────────────────────────────────

    private JPanel buildContent() {
        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setOpaque(false);

        root.add(buildTabBar(), BorderLayout.NORTH);

        tabCards = new CardLayout();
        tabContent = new JPanel(tabCards);
        tabContent.setOpaque(false);

        tabContent.add(buildStockCard(),      "stock");
        tabContent.add(buildLowStockCard(),   "lowstock");
        tabContent.add(buildPlaceOrderCard(), "placeorder");
        tabContent.add(buildHistoryCard(),    "history");
        tabContent.add(buildDeliveriesCard(), "deliveries");

        root.add(tabContent, BorderLayout.CENTER);
        return root;
    }

    private JPanel buildTabBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(0, 0, 4, 0));

        stockTabBtn      = createTabButton("Stock",         "stock");
        lowStockTabBtn   = createTabButton("Low stock",     "lowstock");

        JLabel sep = new JLabel("·");
        sep.setFont(new Font("SansSerif", Font.BOLD, 18));
        sep.setForeground(new Color(160, 160, 160));
        sep.setBorder(new EmptyBorder(0, 6, 0, 6));

        placeOrderTabBtn = createTabButton("Place order",   "placeorder");
        historyTabBtn    = createTabButton("Order history", "history");
        deliveriesTabBtn = createTabButton("Deliveries",    "deliveries");

        bar.add(stockTabBtn);
        bar.add(lowStockTabBtn);
        bar.add(sep);
        bar.add(placeOrderTabBtn);
        bar.add(historyTabBtn);
        bar.add(deliveriesTabBtn);

        return bar;
    }

    private JButton createTabButton(String label, String key) {
        JButton btn = new JButton(label);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setBorderPainted(false);
        btn.setFont(new Font("SansSerif", Font.BOLD, 14));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(8, 16, 8, 16));
        styleTabBtn(btn, key.equals(activeTab));

        btn.addActionListener(e -> switchTab(key));
        return btn;
    }

    private void switchTab(String key) {
        activeTab = key;
        tabCards.show(tabContent, key);
        styleTabBtn(stockTabBtn,      "stock".equals(key));
        styleTabBtn(lowStockTabBtn,   "lowstock".equals(key));
        styleTabBtn(placeOrderTabBtn, "placeorder".equals(key));
        styleTabBtn(historyTabBtn,    "history".equals(key));
        styleTabBtn(deliveriesTabBtn, "deliveries".equals(key));

        if ("lowstock".equals(key))    loadLowStockTable();
        if ("placeorder".equals(key)) { loadCatalogue(); refreshMerchantStatus(); }
        if ("history".equals(key))    loadOrderHistory();
        if ("deliveries".equals(key)) loadDeliveries();
    }

    private void styleTabBtn(JButton btn, boolean active) {
        if (btn == null) return;
        if (active) {
            btn.setForeground(ThemeManager.textPrimary());
            btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 2, 0, ThemeManager.textPrimary()),
                    new EmptyBorder(8, 16, 6, 16)
            ));
        } else {
            btn.setForeground(ThemeManager.textSecondary());
            btn.setBorder(new EmptyBorder(8, 16, 8, 16));
        }
    }

    // ─────────────────────────────────────────────────
    // Stock tab
    // ─────────────────────────────────────────────────

    private JPanel buildStockCard() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);

        tableCard = AppShell.createCard();
        tableCard.setLayout(new BorderLayout(18, 18));
        tableCard.setBorder(new EmptyBorder(20, 20, 20, 20));

        buildToolbar();
        buildStockTable();
        buildFooter();
        buildBottomActionBar();

        tableCard.add(topToolbar,    BorderLayout.NORTH);
        tableCard.add(stockScrollPane, BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout(12, 12));
        south.setOpaque(false);
        south.add(footerPanel,      BorderLayout.NORTH);
        south.add(bottomActionBar,  BorderLayout.SOUTH);
        tableCard.add(south, BorderLayout.SOUTH);

        warningLabel = new JLabel(" ");
        warningLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        warningLabel.setBorder(new EmptyBorder(0, 4, 0, 0));

        panel.add(tableCard,    BorderLayout.CENTER);
        panel.add(warningLabel, BorderLayout.SOUTH);
        return panel;
    }

    private void buildToolbar() {
        topToolbar = new JPanel(new BorderLayout());
        topToolbar.setOpaque(false);

        orderStockBtn = createPillButton("+  Add Stock", true);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);
        left.add(orderStockBtn);

        searchField = new JTextField("Search products...") {
            @Override
            public void addNotify() {
                super.addNotify();
                setForeground(ThemeManager.textSecondary());
                addFocusListener(new java.awt.event.FocusAdapter() {
                    @Override public void focusGained(java.awt.event.FocusEvent e) {
                        if (getText().equals("Search products...")) {
                            setText(""); setForeground(ThemeManager.textPrimary());
                        }
                    }
                    @Override public void focusLost(java.awt.event.FocusEvent e) {
                        if (getText().trim().isEmpty()) {
                            setText("Search products..."); setForeground(ThemeManager.textSecondary());
                        }
                    }
                });
            }
        };
        searchField.setPreferredSize(new Dimension(200, 36));
        searchField.setFont(new Font("SansSerif", Font.PLAIN, 13));
        left.add(searchField);

        filterCombo = new JComboBox<>(new String[]{"All Stocks", "Good", "Low", "Restock"});
        sortCombo   = new JComboBox<>(new String[]{"Sort by: Quantity", "Sort by: Stock ID", "Sort by: Price", "Sort by: Description"});

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        right.add(filterCombo);
        right.add(sortCombo);

        topToolbar.add(left,  BorderLayout.WEST);
        topToolbar.add(right, BorderLayout.EAST);
    }

    private void buildStockTable() {
        stockModel = new DefaultTableModel(
                new String[]{"Stock ID", "Description", "Stock Quantity", "Price", "Rate of Vat", "Status"}, 0
        ) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        stockTable = new JTable(stockModel);
        configureTable(stockTable);
        stockTable.getColumnModel().getColumn(5).setCellRenderer(new StatusCellRenderer());
        stockScrollPane = new JScrollPane(stockTable);
        styleScrollPane(stockScrollPane);
    }

    private void buildFooter() {
        footerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        footerPanel.setOpaque(false);
        for (String s : new String[]{"1","2","3","4","5","...","20"}) {
            footerPanel.add(createPageChip(s, "1".equals(s)));
        }
    }

    private void buildBottomActionBar() {
        bottomActionBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        bottomActionBar.setOpaque(false);
        editBtn    = createPillButton("Edit Product",    false);
        deleteBtn  = createPillButton("Remove Product",  false);
        restockBtn = createPillButton("Restock",         false);
        refreshBtn = createPillButton("Refresh",         false);
        bottomActionBar.add(editBtn);
        bottomActionBar.add(deleteBtn);
        bottomActionBar.add(restockBtn);
        bottomActionBar.add(refreshBtn);
    }

    private void wireActions() {
        refreshBtn.addActionListener(e -> loadTable());
        orderStockBtn.addActionListener(e -> {
            if (!Session.isManagerOrAdmin()) {
                JOptionPane.showMessageDialog(this, "Only Managers and Admins can add stock.");
                return;
            }
            showAddDialog();
        });

        editBtn.addActionListener(e -> {
            if (!Session.isManagerOrAdmin()) {
                JOptionPane.showMessageDialog(this, "Only Managers and Admins can edit products.");
                return;
            }
            int selectedIndex = getSelectedVisibleIndex();
            if (selectedIndex == -1) {
                JOptionPane.showMessageDialog(this, "Please select a product to edit.");
                return;
            }
            showEditDialog(visibleProducts.get(selectedIndex));
        });

        deleteBtn.addActionListener(e -> {
            if (!Session.isManagerOrAdmin()) {
                JOptionPane.showMessageDialog(this, "Only Managers and Admins can remove products.");
                return;
            }
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
            int i = getSelectedVisibleIndex();
            if (i == -1) { JOptionPane.showMessageDialog(this, "Please select a product to restock."); return; }
            Product p = visibleProducts.get(i);
            String input = JOptionPane.showInputDialog(this, "Quantity to add:");
            if (input == null || input.trim().isEmpty()) return;
            try {
                int qty = Integer.parseInt(input.trim());
                if (qty <= 0) throw new NumberFormatException();
                ProductDB.updateStock(p.getProductId(), qty);
                loadStockTable();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter a valid positive quantity.");
            }
        });

        filterCombo.addActionListener(e -> refreshTableView());
        sortCombo.addActionListener(e -> refreshTableView());
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { refreshTableView(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { refreshTableView(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { refreshTableView(); }
        });

        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentShown(java.awt.event.ComponentEvent e) { loadStockTable(); }
        });
    }

    private int getSelectedVisibleIndex() {
        int row = stockTable.getSelectedRow();
        return row == -1 ? -1 : stockTable.convertRowIndexToModel(row);
    }

    private void loadStockTable() {
        allProducts.clear();
        allProducts.addAll(ProductDB.getAllProducts());
        refreshTableView();
        int low = ProductDB.getLowStockCount();
        warningLabel.setText(low > 0
                ? "Warning: " + low + " product(s) are at or below minimum stock."
                : "All products are above minimum stock.");
    }

    private void refreshTableView() {
        stockModel.setRowCount(0);
        visibleProducts.clear();

        String sel  = filterCombo != null ? (String) filterCombo.getSelectedItem() : "All Stocks";
        String sort = sortCombo   != null ? (String) sortCombo.getSelectedItem()   : "Sort by: Quantity";
        String kw   = searchField != null && !searchField.getText().equals("Search products...")
                ? searchField.getText().trim().toLowerCase() : "";

        List<Product> filtered = new ArrayList<>();
        for (Product p : allProducts) {
            String status = getStatusText(p);
            boolean matchSearch = kw.isEmpty()
                    || p.getDescription().toLowerCase().contains(kw)
                    || p.getItemId().toLowerCase().contains(kw);
            boolean matchFilter = "All Stocks".equals(sel) || status.equalsIgnoreCase(sel);
            if (matchSearch && matchFilter) filtered.add(p);
        }

        if      ("Sort by: Quantity".equals(sort))    filtered.sort(Comparator.comparingInt(Product::getStockQuantity));
        else if ("Sort by: Stock ID".equals(sort))    filtered.sort(Comparator.comparingInt(Product::getProductId));
        else if ("Sort by: Price".equals(sort))       filtered.sort(Comparator.comparingDouble(Product::getPrice));
        else if ("Sort by: Description".equals(sort)) filtered.sort(Comparator.comparing(Product::getDescription, String.CASE_INSENSITIVE_ORDER));

        visibleProducts.addAll(filtered);
        for (Product p : visibleProducts) {
            stockModel.addRow(new Object[]{
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

    private String getStatusText(Product p) {
        int stock = p.getStockQuantity(), min = p.getMinStockLevel();
        if (stock <= min)      return "Restock";
        if (stock <= min + 5)  return "Low";
        return "Good";
    }

    // ─────────────────────────────────────────────────
    // Low stock tab
    // ─────────────────────────────────────────────────

    private JPanel buildLowStockCard() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setOpaque(false);

        JPanel card = AppShell.createCard();
        card.setLayout(new BorderLayout(12, 12));
        card.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Products at or below minimum stock");
        title.setFont(new Font("SansSerif", Font.BOLD, 16));

        lowStockModel = new DefaultTableModel(
                new String[]{"Stock ID", "Description", "Current Stock", "Min Stock", "Status"}, 0
        ) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        lowStockTable = new JTable(lowStockModel);
        configureTable(lowStockTable);
        lowStockScroll = new JScrollPane(lowStockTable);
        styleScrollPane(lowStockScroll);

        JButton goPlaceOrder = createRoundedButton("Place restock order →", true);
        goPlaceOrder.addActionListener(e -> switchTab("placeorder"));

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        bottom.setOpaque(false);
        bottom.add(goPlaceOrder);

        card.add(title,          BorderLayout.NORTH);
        card.add(lowStockScroll, BorderLayout.CENTER);
        card.add(bottom,         BorderLayout.SOUTH);

        panel.add(card, BorderLayout.CENTER);
        return panel;
    }

    private void loadLowStockTable() {
        lowStockModel.setRowCount(0);
        for (Product p : ProductDB.getAllProducts()) {
            if (p.getStockQuantity() <= p.getMinStockLevel() + 5) {
                lowStockModel.addRow(new Object[]{
                        String.format("%03d", p.getProductId()),
                        p.getDescription(),
                        p.getStockQuantity(),
                        p.getMinStockLevel(),
                        getStatusText(p)
                });
            }
        }
        applyTableTheme(lowStockTable);
    }

    // ─────────────────────────────────────────────────
    // Place order tab
    // ─────────────────────────────────────────────────

    private JPanel buildPlaceOrderCard() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);

        // Merchant status strip
        JPanel merchantStrip = AppShell.createCard();
        merchantStrip.setLayout(new FlowLayout(FlowLayout.LEFT, 18, 6));

        JLabel merchantStatLabel = new JLabel("Merchant Account:");
        merchantStatLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        JLabel activeLabel  = new JLabel("Active Orders:");
        activeOrdersValue   = new JLabel("—");
        JLabel outLabel     = new JLabel("Outstanding Value:");
        outstandingValue    = new JLabel("—");

        JButton balanceBtn = createRoundedButton("Check Balance", false);
        balanceBtn.addActionListener(e -> showAccountBalanceDialog());

        merchantStrip.add(merchantStatLabel);
        merchantStrip.add(Box.createHorizontalStrut(12));
        merchantStrip.add(activeLabel);
        merchantStrip.add(activeOrdersValue);
        merchantStrip.add(Box.createHorizontalStrut(18));
        merchantStrip.add(outLabel);
        merchantStrip.add(outstandingValue);
        merchantStrip.add(Box.createHorizontalStrut(18));
        merchantStrip.add(balanceBtn);

        // Controls row
        JPanel controls = new JPanel(new BorderLayout(20, 0));
        controls.setOpaque(false);

        JPanel leftBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        leftBtns.setOpaque(false);
        addToOrderBtn = createRoundedButton("+  Add To Order", true);
        removeItemBtn = createRoundedButton("Remove Item",     false);
        leftBtns.add(addToOrderBtn);
        leftBtns.add(removeItemBtn);

        JPanel rightMerchant = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        rightMerchant.setOpaque(false);
        JLabel midLabel = new JLabel("Merchant ID:");
        merchantIdValue = new JLabel("MERCHANT-001");
        merchantIdValue.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.borderColor()),
                new EmptyBorder(10, 14, 10, 14)
        ));
        rightMerchant.add(midLabel);
        rightMerchant.add(merchantIdValue);
        controls.add(leftBtns,      BorderLayout.WEST);
        controls.add(rightMerchant, BorderLayout.EAST);

        // Two-column table area
        JPanel tables = new JPanel(new GridLayout(1, 2, 20, 0));
        tables.setOpaque(false);

        JPanel leftCard = AppShell.createCard();
        leftCard.setLayout(new BorderLayout(12, 12));
        JLabel availLabel = new JLabel("Available Products");
        availLabel.setFont(new Font("SansSerif", Font.BOLD, 16));

        productsModel = new DefaultTableModel(
                new String[]{"ID", "Item ID", "Description", "Price £", "Stock", "Min Stock"}, 0
        ) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        productsTable = new JTable(productsModel);
        configureTable(productsTable);
        productsScrollPane = new JScrollPane(productsTable);
        styleScrollPane(productsScrollPane);

        leftCard.add(availLabel,          BorderLayout.NORTH);
        leftCard.add(productsScrollPane,  BorderLayout.CENTER);

        JPanel rightCard = AppShell.createCard();
        rightCard.setLayout(new BorderLayout(12, 12));
        JLabel summaryLabel = new JLabel("Order Summary");
        summaryLabel.setFont(new Font("SansSerif", Font.BOLD, 16));

        summaryModel = new DefaultTableModel(
                new String[]{"Description", "Qty", "Unit Cost £", "Line Total £"}, 0
        ) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        summaryTable = new JTable(summaryModel);
        configureTable(summaryTable);
        summaryScrollPane = new JScrollPane(summaryTable);
        styleScrollPane(summaryScrollPane);

        totalLabel = new JLabel("Total: £0.00");
        totalLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        JPanel totalWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        totalWrap.setOpaque(false);
        totalWrap.add(totalLabel);

        JPanel rightInner = new JPanel(new BorderLayout(12, 12));
        rightInner.setOpaque(false);
        rightInner.add(summaryLabel,    BorderLayout.NORTH);
        rightInner.add(summaryScrollPane, BorderLayout.CENTER);
        rightInner.add(totalWrap,       BorderLayout.SOUTH);
        rightCard.add(rightInner, BorderLayout.CENTER);

        tables.add(leftCard);
        tables.add(rightCard);

        // Bottom bar
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        bottomBar.setOpaque(false);
        clearOrderBtn = createRoundedButton("Clear Order", false);
        placeOrderBtn = createRoundedButton("Place Order", true);
        bottomBar.add(clearOrderBtn);
        bottomBar.add(placeOrderBtn);

        wirePlaceOrderActions();

        JPanel inner = new JPanel(new BorderLayout(0, 12));
        inner.setOpaque(false);
        inner.add(controls,  BorderLayout.NORTH);
        inner.add(tables,    BorderLayout.CENTER);
        inner.add(bottomBar, BorderLayout.SOUTH);

        panel.add(merchantStrip, BorderLayout.NORTH);
        panel.add(inner,         BorderLayout.CENTER);
        return panel;
    }

    private void wirePlaceOrderActions() {
        addToOrderBtn.addActionListener(e -> addSelectedProduct());
        removeItemBtn.addActionListener(e -> removeSelectedSummaryItem());
        clearOrderBtn.addActionListener(e -> clearOrder());
        placeOrderBtn.addActionListener(e -> placeOrder());
    }

    private void loadCatalogue() {
        productsModel.setRowCount(0);
        for (Product p : ProductDB.getAllProducts()) {
            productsModel.addRow(new Object[]{
                    p.getProductId(), p.getItemId(), p.getDescription(),
                    String.format("%.2f", p.getPrice()),
                    p.getStockQuantity(), p.getMinStockLevel()
            });
        }
        applyTableTheme(productsTable);
    }

    private void refreshMerchantStatus() {
        if (activeOrdersValue == null) return;
        activeOrdersValue.setText(String.valueOf(RestockOrderDB.getActiveOrderCount()));
        outstandingValue.setText(String.format("£%.2f", RestockOrderDB.getOutstandingValue()));
    }

    private void addSelectedProduct() {
        int row = productsTable.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this, "Please select a product."); return; }
        int modelRow  = productsTable.convertRowIndexToModel(row);
        int productId = (int) productsModel.getValueAt(modelRow, 0);
        Product p = ProductDB.getById(productId);
        if (p == null) { JOptionPane.showMessageDialog(this, "Could not load the selected product."); return; }

        String input = JOptionPane.showInputDialog(this, "Quantity to order:");
        if (input == null || input.trim().isEmpty()) return;
        try {
            int qty = Integer.parseInt(input.trim());
            if (qty <= 0) throw new NumberFormatException();
            cart.merge(p, qty, Integer::sum);
            refreshSummary();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid positive quantity.");
        }
    }

    private void removeSelectedSummaryItem() {
        int row = summaryTable.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this, "Please select an item to remove."); return; }
        Product key = (Product) cart.keySet().toArray()[summaryTable.convertRowIndexToModel(row)];
        cart.remove(key);
        refreshSummary();
    }

    private void refreshSummary() {
        summaryModel.setRowCount(0);
        double total = 0;
        for (Map.Entry<Product, Integer> e : cart.entrySet()) {
            Product p = e.getKey(); int qty = e.getValue();
            double line = p.getPrice() * qty;
            total += line;
            summaryModel.addRow(new Object[]{
                    p.getDescription(), qty,
                    String.format("%.2f", p.getPrice()),
                    String.format("%.2f", line)
            });
        }
        totalLabel.setText("Total: £" + String.format("%.2f", total));
    }

    private void clearOrder() { cart.clear(); refreshSummary(); }

    private void placeOrder() {
        if (cart.isEmpty()) { JOptionPane.showMessageDialog(this, "Order is empty."); return; }

        String merchantId = JOptionPane.showInputDialog(this, "Merchant ID:", merchantIdValue.getText());
        if (merchantId == null || merchantId.trim().isEmpty()) return;
        merchantId = merchantId.trim();
        merchantIdValue.setText(merchantId);

        double outstanding = RestockOrderDB.getOutstandingValue();
        if (outstanding > 10_000.00) {
            JOptionPane.showMessageDialog(this,
                    "Cannot place order. Outstanding balance (£" + String.format("%.2f", outstanding) +
                    ") exceeds the allowed limit. Please settle your account first.",
                    "Account Restricted", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String orderNumber = RestockOrderDB.placeOrder(merchantId, cart);
        if (orderNumber != null) {
            refreshMerchantStatus();
            clearOrder();
            loadCatalogue();
            int print = JOptionPane.showConfirmDialog(this,
                    "Order placed successfully.\nOrder number: " + orderNumber +
                    "\n\nWould you like to print the order form?",
                    "Order Placed", JOptionPane.YES_NO_OPTION);
            if (print == JOptionPane.YES_OPTION) {
                RestockOrder placed = RestockOrderDB.getByOrderNumber(orderNumber);
                if (placed != null) {
                    List<RestockOrderItem> items = RestockOrderDB.getOrderItems(placed.getRestockOrderId());
                    PdfGenerator.generateOrderForm(this, placed, items);
                }
            }
        } else {
            JOptionPane.showMessageDialog(this, "Failed to place order.");
        }
    }

    private void showAccountBalanceDialog() {
        int active = RestockOrderDB.getActiveOrderCount();
        double out = RestockOrderDB.getOutstandingValue();
        JOptionPane.showMessageDialog(this,
                String.format("Merchant Account Summary\n\nActive Orders:       %d\nOutstanding Balance: £%.2f\n\n" +
                        "Note: Balance represents the total value of\naccepted/processed/dispatched orders not yet delivered.",
                        active, out),
                "Account Balance", JOptionPane.INFORMATION_MESSAGE);
    }

    // ─────────────────────────────────────────────────
    // Order history tab
    // ─────────────────────────────────────────────────

    private JPanel buildHistoryCard() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setOpaque(false);

        JPanel card = AppShell.createCard();
        card.setLayout(new BorderLayout(12, 12));
        card.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Restock Order History");
        title.setFont(new Font("SansSerif", Font.BOLD, 16));

        historyModel = new DefaultTableModel(
                new String[]{"ID", "Order Number", "Merchant ID", "Status", "Total £", "Created At"}, 0
        ) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        historyTable = new JTable(historyModel);
        configureTable(historyTable);
        historyScroll = new JScrollPane(historyTable);
        styleScrollPane(historyScroll);

        JButton refreshHistoryBtn = createRoundedButton("Refresh",          false);
        JButton viewItemsBtn      = createRoundedButton("View Items",       false);
        JButton printBtn          = createRoundedButton("Print Order Form", false);
        JButton updateStatusBtn   = createRoundedButton("Update Status",    true);
        JButton viewInvoiceBtn    = createRoundedButton("View Invoice",     false);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        buttons.setOpaque(false);
        buttons.add(refreshHistoryBtn);
        buttons.add(viewItemsBtn);
        buttons.add(printBtn);
        buttons.add(updateStatusBtn);
        buttons.add(viewInvoiceBtn);

        refreshHistoryBtn.addActionListener(e -> loadOrderHistory());

        viewItemsBtn.addActionListener(e -> {
            int row = historyTable.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(this, "Please select an order."); return; }
            int orderId = (int) historyModel.getValueAt(historyTable.convertRowIndexToModel(row), 0);
            showOrderItemsDialog(SwingUtilities.getWindowAncestor(this), orderId);
        });

        printBtn.addActionListener(e -> {
            int row = historyTable.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(this, "Please select an order."); return; }
            int mr = historyTable.convertRowIndexToModel(row);
            String orderNum = String.valueOf(historyModel.getValueAt(mr, 1));
            RestockOrder order = RestockOrderDB.getByOrderNumber(orderNum);
            if (order == null) return;
            List<RestockOrderItem> items = RestockOrderDB.getOrderItems(order.getRestockOrderId());
            PdfGenerator.generateOrderForm(this, order, items);
        });

        viewInvoiceBtn.addActionListener(e -> {
            int row = historyTable.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(this, "Please select an order."); return; }
            int mr = historyTable.convertRowIndexToModel(row);
            showOrderInvoiceDialog(
                    SwingUtilities.getWindowAncestor(this),
                    String.valueOf(historyModel.getValueAt(mr, 1)),
                    String.valueOf(historyModel.getValueAt(mr, 2)),
                    String.valueOf(historyModel.getValueAt(mr, 3)),
                    String.valueOf(historyModel.getValueAt(mr, 4)),
                    String.valueOf(historyModel.getValueAt(mr, 5))
            );
        });

        updateStatusBtn.addActionListener(e -> {
            int row = historyTable.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(this, "Please select an order."); return; }
            int mr = historyTable.convertRowIndexToModel(row);
            int orderId = (int) historyModel.getValueAt(mr, 0);
            String current = String.valueOf(historyModel.getValueAt(mr, 3));
            String next = getNextStatus(current);
            if (next == null) { JOptionPane.showMessageDialog(this, "This order is already delivered."); return; }

            if (JOptionPane.showConfirmDialog(this,
                    "Update status from " + current + " to " + next + "?",
                    "Confirm status change", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                RestockOrderDB.updateStatus(orderId, current, next);
                if ("DELIVERED".equals(next)) {
                    List<RestockOrderItem> delivered = RestockOrderDB.getOrderItems(orderId);
                    for (RestockOrderItem item : delivered) {
                        Product p = ProductDB.getByItemId(item.getItemId());
                        if (p != null) ProductDB.updateStock(p.getProductId(), item.getQuantity());
                    }
                    JOptionPane.showMessageDialog(this,
                            "Delivery recorded. Stock updated for " + delivered.size() + " product(s).");
                    loadStockTable();
                }
                loadOrderHistory();
                loadDeliveries();
            }
        });

        card.add(title,         BorderLayout.NORTH);
        card.add(historyScroll, BorderLayout.CENTER);
        card.add(buttons,       BorderLayout.SOUTH);

        panel.add(card, BorderLayout.CENTER);
        return panel;
    }

    private void loadOrderHistory() {
        if (historyModel == null) return;
        historyModel.setRowCount(0);
        for (RestockOrder o : RestockOrderDB.getAllOrders()) {
            historyModel.addRow(new Object[]{
                    o.getRestockOrderId(), o.getOrderNumber(), o.getMerchantId(),
                    o.getStatus(), String.format("%.2f", o.getTotalValue()), o.getCreatedAt()
            });
        }
        applyTableTheme(historyTable);
    }

    private String getNextStatus(String current) {
        return switch (current) {
            case "ACCEPTED"  -> "PROCESSED";
            case "PROCESSED" -> "DISPATCHED";
            case "DISPATCHED"-> "DELIVERED";
            default          -> null;
        };
    }

    private void showOrderItemsDialog(Window parent, int restockOrderId) {
        List<RestockOrderItem> items = RestockOrderDB.getOrderItems(restockOrderId);

        DefaultTableModel itemModel = new DefaultTableModel(
                new String[]{"Item ID", "Description", "Qty", "Unit Cost £", "Line Total £"}, 0
        ) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        double grand = 0;
        for (RestockOrderItem item : items) {
            itemModel.addRow(new Object[]{
                    item.getItemId(), item.getDescription(), item.getQuantity(),
                    String.format("%.2f", item.getUnitCost()),
                    String.format("%.2f", item.getLineTotal())
            });
            grand += item.getLineTotal();
        }

        JTable t = new JTable(itemModel);
        configureTable(t); applyTableTheme(t);
        JScrollPane sp = new JScrollPane(t); styleScrollPane(sp);
        sp.setPreferredSize(new Dimension(600, 250));

        JLabel totalLbl = new JLabel("Grand Total: £" + String.format("%.2f", grand));
        totalLbl.setFont(new Font("SansSerif", Font.BOLD, 13));
        totalLbl.setBorder(new EmptyBorder(8, 0, 0, 0));

        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(new EmptyBorder(12, 12, 12, 12));
        content.setBackground(ThemeManager.panelBackground());
        content.add(sp,       BorderLayout.CENTER);
        content.add(totalLbl, BorderLayout.SOUTH);

        JDialog d = new JDialog(parent, "Order Items", Dialog.ModalityType.APPLICATION_MODAL);
        d.setContentPane(content);
        d.pack();
        d.setLocationRelativeTo(parent);
        d.setVisible(true);
    }

    private void showOrderInvoiceDialog(Window parent, String orderNum, String merchantId,
                                        String status, String total, String date) {
        String invoice = String.format(
                "ORDER INVOICE\n═══════════════════════════════\n" +
                "Order Number : %s\nMerchant ID  : %s\nDate         : %s\n" +
                "Status       : %s\n───────────────────────────────\n" +
                "Total Value  : £%s\n═══════════════════════════════\n\n" +
                "Payment is due by end of calendar month.",
                orderNum, merchantId, date, status, total);

        JTextArea area = new JTextArea(invoice);
        area.setEditable(false);
        area.setFont(new Font("Monospaced", Font.PLAIN, 13));
        area.setMargin(new Insets(12, 12, 12, 12));
        JOptionPane.showMessageDialog(parent, new JScrollPane(area),
                "Invoice — " + orderNum, JOptionPane.PLAIN_MESSAGE);
    }

    // ─────────────────────────────────────────────────
    // Deliveries tab
    // ─────────────────────────────────────────────────

    private JPanel buildDeliveriesCard() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setOpaque(false);

        JPanel card = AppShell.createCard();
        card.setLayout(new BorderLayout(12, 12));
        card.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Pending & Recent Deliveries");
        title.setFont(new Font("SansSerif", Font.BOLD, 16));

        deliveriesModel = new DefaultTableModel(
                new String[]{"ID", "Order Number", "Merchant ID", "Status", "Total £", "Created At"}, 0
        ) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        deliveriesTable = new JTable(deliveriesModel);
        configureTable(deliveriesTable);
        deliveriesScroll = new JScrollPane(deliveriesTable);
        styleScrollPane(deliveriesScroll);

        JButton refreshBtn2 = createRoundedButton("Refresh", false);
        refreshBtn2.addActionListener(e -> loadDeliveries());

        JPanel bottom2 = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        bottom2.setOpaque(false);
        bottom2.add(refreshBtn2);

        card.add(title,            BorderLayout.NORTH);
        card.add(deliveriesScroll, BorderLayout.CENTER);
        card.add(bottom2,          BorderLayout.SOUTH);

        panel.add(card, BorderLayout.CENTER);
        return panel;
    }

    private void loadDeliveries() {
        if (deliveriesModel == null) return;
        deliveriesModel.setRowCount(0);
        for (RestockOrder o : RestockOrderDB.getAllOrders()) {
            String s = o.getStatus();
            if ("DISPATCHED".equals(s) || "DELIVERED".equals(s) || "PROCESSED".equals(s)) {
                deliveriesModel.addRow(new Object[]{
                        o.getRestockOrderId(), o.getOrderNumber(), o.getMerchantId(),
                        s, String.format("%.2f", o.getTotalValue()), o.getCreatedAt()
                });
            }
        }
        applyTableTheme(deliveriesTable);
    }

    // ─────────────────────────────────────────────────
    // Shared add/edit dialogs (Stock tab)
    // ─────────────────────────────────────────────────

    private void showAddDialog() {
        Window w = SwingUtilities.getWindowAncestor(this);
        ProductFormDialog d = new ProductFormDialog(w, "Order / Add Stock", true, null);
        d.setVisible(true);
        if (!d.isConfirmed()) return;
        try {
            Product p = new Product(0, d.getItemId(), d.getDescriptionText(), d.getPackageType(),
                    d.getUnitsInPack(), d.getPrice(), d.getVatRate(), d.getStockQuantity(), d.getMinimumStock());
            if (ProductDB.addProduct(p)) loadStockTable();
            else JOptionPane.showMessageDialog(this, "Could not add product. Check that the Item ID is unique.");
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please check the numeric fields.");
        }
    }

    private void showEditDialog(Product product) {
        Product fresh = ProductDB.getById(product.getProductId());
        if (fresh == null) { JOptionPane.showMessageDialog(this, "Could not load the selected product."); return; }
        Window w = SwingUtilities.getWindowAncestor(this);
        ProductFormDialog d = new ProductFormDialog(w, "Edit Product", false, fresh);
        d.setVisible(true);
        if (!d.isConfirmed()) return;
        try {
            fresh.setDescription(d.getDescriptionText());
            fresh.setPackageType(d.getPackageType());
            fresh.setUnitsInPack(d.getUnitsInPack());
            fresh.setPrice(d.getPrice());
            fresh.setVatRate(d.getVatRate());
            fresh.setStockQuantity(d.getStockQuantity());
            fresh.setMinStockLevel(d.getMinimumStock());
            if (ProductDB.updateProduct(fresh)) loadStockTable();
            else JOptionPane.showMessageDialog(this, "Could not update the product.");
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please check the numeric fields.");
        }
    }

    // ─────────────────────────────────────────────────
    // Shared helpers
    // ─────────────────────────────────────────────────

    private void configureTable(JTable t) {
        t.setRowHeight(42);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setFillsViewportHeight(true);
        t.setBorder(BorderFactory.createEmptyBorder());
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setDefaultEditor(Object.class, null);
        t.setFont(new Font("SansSerif", Font.PLAIN, 13));
        t.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        JTableHeader h = t.getTableHeader();
        h.setReorderingAllowed(false);
        h.setResizingAllowed(false);
        h.setFont(new Font("SansSerif", Font.BOLD, 13));
        h.setBorder(BorderFactory.createEmptyBorder());
        h.setPreferredSize(new Dimension(h.getPreferredSize().width, 40));

        DefaultTableCellRenderer r = new DefaultTableCellRenderer();
        r.setBorder(new EmptyBorder(0, 10, 0, 10));
        r.setHorizontalAlignment(SwingConstants.LEFT);
        for (int i = 0; i < t.getColumnCount(); i++) t.getColumnModel().getColumn(i).setCellRenderer(r);
    }

    private void styleScrollPane(JScrollPane sp) {
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setOpaque(true);
        sp.setBackground(ThemeManager.tableBackground());
        sp.getViewport().setBackground(ThemeManager.tableBackground());
        sp.getViewport().setBorder(null);
        sp.getViewport().setOpaque(true);
    }

    private void applyTableTheme(JTable t) {
        if (t == null) return;
        t.setBackground(ThemeManager.tableBackground());
        t.setForeground(ThemeManager.textPrimary());
        t.setSelectionBackground(ThemeManager.selectionBackground());
        t.setSelectionForeground(ThemeManager.textPrimary());

        JTableHeader h = t.getTableHeader();
        if (h != null) {
            h.setBackground(ThemeManager.tableBackground());
            h.setForeground(ThemeManager.textPrimary());
            h.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ThemeManager.borderColor()));
            h.setOpaque(true);
        }
        t.repaint();
    }

    private JButton createPillButton(String text, boolean primary) {
        JButton btn = new JButton(text);
        btn.setFocusPainted(false);
        btn.setOpaque(true);
        btn.setContentAreaFilled(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setBorder(new EmptyBorder(10, 18, 10, 18));
        stylePillButton(btn, primary);
        return btn;
    }

    private void stylePillButton(JButton btn, boolean primary) {
        if (primary) {
            btn.setBackground(ThemeManager.buttonDark());
            btn.setForeground(ThemeManager.textLight());
            btn.setBorder(new EmptyBorder(10, 18, 10, 18));
        } else {
            btn.setBackground(ThemeManager.buttonLight());
            btn.setForeground(ThemeManager.textPrimary());
            btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ThemeManager.borderColor()),
                    new EmptyBorder(9, 16, 9, 16)
            ));
        }
    }

    private JButton createRoundedButton(String text, boolean primary) {
        JButton btn = new RoundedButton(text);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setBorder(BorderFactory.createEmptyBorder(12, 22, 12, 22));
        if (primary) {
            btn.setBackground(ThemeManager.buttonDark());
            btn.setForeground(ThemeManager.textLight());
            btn.putClientProperty("outlineColor", ThemeManager.buttonDark());
        } else {
            btn.setBackground(ThemeManager.buttonLight());
            btn.setForeground(ThemeManager.textPrimary());
            btn.putClientProperty("outlineColor", ThemeManager.borderColor());
        }
        return btn;
    }

    private JButton createPageChip(String text, boolean active) {
        JButton btn = new JButton(text);
        btn.setFocusPainted(false);
        btn.setOpaque(true);
        btn.setContentAreaFilled(true);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setPreferredSize(new Dimension(26, 24));
        btn.setBorder(BorderFactory.createEmptyBorder());
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (active) {
            btn.setBackground(new Color(77, 77, 77));
            btn.setForeground(Color.WHITE);
        } else {
            btn.setBackground(ThemeManager.isDark() ? new Color(55, 58, 66) : new Color(245, 245, 245));
            btn.setForeground(ThemeManager.textSecondary());
        }
        return btn;
    }

    private void styleComboBox(JComboBox<String> cb) {
        cb.setFocusable(false);
        cb.setFont(new Font("SansSerif", Font.PLAIN, 13));
        cb.setPreferredSize(new Dimension(130, 30));
        cb.setBackground(ThemeManager.comboBackground());
        cb.setForeground(ThemeManager.comboForeground());
        cb.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.borderColor()),
                new EmptyBorder(4, 8, 4, 8)
        ));
    }

    // ─────────────────────────────────────────────────
    // Theme
    // ─────────────────────────────────────────────────

    @Override
    public void applyTheme() {
        setBackground(ThemeManager.appBackground());

        if (tableCard != null)       tableCard.setBackground(ThemeManager.panelBackground());
        if (topToolbar != null)      topToolbar.setBackground(ThemeManager.panelBackground());
        if (bottomActionBar != null) bottomActionBar.setBackground(ThemeManager.panelBackground());
        if (footerPanel != null)     footerPanel.setBackground(ThemeManager.panelBackground());

        if (warningLabel != null)
            warningLabel.setForeground(ProductDB.getLowStockCount() > 0
                    ? new Color(190, 76, 76) : ThemeManager.textSecondary());

        if (orderStockBtn != null) stylePillButton(orderStockBtn, true);
        if (editBtn != null)       stylePillButton(editBtn,       false);
        if (deleteBtn != null)     stylePillButton(deleteBtn,     false);
        if (restockBtn != null)    stylePillButton(restockBtn,    false);
        if (refreshBtn != null)    stylePillButton(refreshBtn,    false);

        if (filterCombo != null) styleComboBox(filterCombo);
        if (sortCombo != null)   styleComboBox(sortCombo);

        if (stockScrollPane != null) styleScrollPane(stockScrollPane);
        applyTableTheme(stockTable);
        applyTableTheme(lowStockTable);
        applyTableTheme(productsTable);
        applyTableTheme(summaryTable);
        applyTableTheme(historyTable);
        applyTableTheme(deliveriesTable);

        styleTabBtn(stockTabBtn,      "stock".equals(activeTab));
        styleTabBtn(lowStockTabBtn,   "lowstock".equals(activeTab));
        styleTabBtn(placeOrderTabBtn, "placeorder".equals(activeTab));
        styleTabBtn(historyTabBtn,    "history".equals(activeTab));
        styleTabBtn(deliveriesTabBtn, "deliveries".equals(activeTab));

        repaint();
        revalidate();
    }

    // ─────────────────────────────────────────────────
    // Inner classes
    // ─────────────────────────────────────────────────

    private class StatusCellRenderer extends DefaultTableCellRenderer {
        public StatusCellRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
            setFont(new Font("SansSerif", Font.BOLD, 12));
            setBorder(new EmptyBorder(0, 10, 0, 10));
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int col) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            String status = value == null ? "" : value.toString();
            setForeground(Color.BLACK);
            if      ("Restock".equalsIgnoreCase(status)) setBackground(new Color(237, 106, 94));
            else if ("Low".equalsIgnoreCase(status))      setBackground(new Color(244, 213, 96));
            else                                          setBackground(new Color(204, 227, 102));
            if (isSelected)
                setBorder(BorderFactory.createLineBorder(ThemeManager.isDark()
                        ? new Color(220, 220, 220) : new Color(80, 80, 80), 1));
            else
                setBorder(new EmptyBorder(0, 10, 0, 10));
            setText(status);
            return this;
        }
    }

    private static class RoundedButton extends JButton {
        public RoundedButton(String text) {
            super(text);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color fill = getBackground();
            if (getModel().isPressed()) fill = fill.darker();
            else if (getModel().isRollover()) fill = adjust(fill, 8);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 26, 26);
            g2.dispose();
            super.paintComponent(g);
        }

        @Override
        protected void paintBorder(Graphics g) {
            Object v = getClientProperty("outlineColor");
            Color outline = v instanceof Color ? (Color) v : new Color(210, 210, 210);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(outline);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 26, 26);
            g2.dispose();
        }

        private Color adjust(Color c, int amt) {
            return new Color(Math.min(255, c.getRed() + amt),
                             Math.min(255, c.getGreen() + amt),
                             Math.min(255, c.getBlue() + amt), c.getAlpha());
        }
    }

    private static class RoundedDarkButtonUI extends javax.swing.plaf.basic.BasicButtonUI {
        @Override public void installUI(JComponent c) {
            super.installUI(c);
            JButton b = (JButton) c;
            b.setOpaque(false);
            b.setBorder(BorderFactory.createEmptyBorder(14, 40, 14, 40));
            b.setRolloverEnabled(true);
        }

        @Override public void paint(Graphics g, JComponent c) {
            JButton b = (JButton) c;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            ButtonModel m = b.getModel();
            Color bg = m.isPressed() ? new Color(22, 24, 33)
                     : m.isRollover() ? new Color(30, 32, 43)
                     : new Color(27, 29, 39);
            int arc = c.getHeight();
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), arc, arc);
            FontMetrics fm = g2.getFontMetrics(b.getFont());
            String text = b.getText();
            int tx = (c.getWidth() - fm.stringWidth(text)) / 2;
            int ty = (c.getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2.setFont(b.getFont());
            g2.setColor(b.getForeground());
            g2.drawString(text, tx, ty);
            g2.dispose();
        }
    }

    private static class ProductFormDialog extends JDialog {
        private final boolean addMode;
        private boolean confirmed = false;
        private JTextField itemIdField, descField, pkgField, unitsField,
                           priceField, vatField, stockField, minStockField;

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
                    new EmptyBorder(20, 24, 20, 24)));

            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0; c.gridy = 0; c.weightx = 1.0; c.fill = GridBagConstraints.HORIZONTAL;

            JLabel titleLabel = new JLabel(addMode ? "Order / Add Stock" : "Edit Product");
            titleLabel.setFont(new Font("SansSerif", Font.BOLD, 28));
            titleLabel.setForeground(ThemeManager.textPrimary());
            c.insets = new Insets(0, 0, 10, 0);
            card.add(titleLabel, c);

            c.gridy++;
            JLabel sub = new JLabel(addMode ? "Enter the product details below to add new stock."
                                            : "Update the product details below.");
            sub.setFont(new Font("SansSerif", Font.PLAIN, 14));
            sub.setForeground(ThemeManager.textSecondary());
            c.insets = new Insets(0, 0, 16, 0);
            card.add(sub, c);

            itemIdField   = createField();
            descField     = createField();
            pkgField      = createField();
            unitsField    = createField();
            priceField    = createField();
            vatField      = createField();
            stockField    = createField();
            minStockField = createField();

            if (product == null) {
                unitsField.setText("1"); vatField.setText("0.00");
                stockField.setText("0"); minStockField.setText("5");
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

            addField(card, c, "Item ID",       itemIdField);
            addField(card, c, "Description",   descField);
            addField(card, c, "Package Type",  pkgField);
            addField(card, c, "Units in Pack", unitsField);
            addField(card, c, "Price £",       priceField);
            addField(card, c, "VAT %",         vatField);
            addField(card, c, "Stock Quantity",stockField);
            addField(card, c, "Minimum Stock", minStockField);

            JPanel btnWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            btnWrap.setOpaque(false);
            JButton confirm = new JButton("Confirm");
            confirm.setFont(new Font("SansSerif", Font.BOLD, 18));
            confirm.setForeground(Color.WHITE);
            confirm.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            confirm.setFocusPainted(false);
            confirm.setContentAreaFilled(false);
            confirm.setBorder(BorderFactory.createEmptyBorder(14, 40, 14, 40));
            confirm.setUI(new RoundedDarkButtonUI());
            confirm.addActionListener(e -> onConfirm());
            btnWrap.add(confirm);

            c.gridy++; c.insets = new Insets(20, 0, 0, 0);
            card.add(btnWrap, c);
            root.add(card);
            getRootPane().setDefaultButton(confirm);
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
            c.gridy++; c.insets = new Insets(0, 0, 10, 0);
            panel.add(block, c);
        }

        private JTextField createField() {
            JTextField f = new JTextField();
            f.setFont(new Font("SansSerif", Font.PLAIN, 15));
            f.setPreferredSize(new Dimension(0, 40));
            f.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ThemeManager.borderColor()),
                    new EmptyBorder(8, 10, 8, 10)));
            return f;
        }

        private void onConfirm() {
            if (itemIdField.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Item ID is required."); return;
            }
            if (descField.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Description is required."); return;
            }
            try {
                Integer.parseInt(unitsField.getText().trim());
                Double.parseDouble(priceField.getText().trim());
                Double.parseDouble(vatField.getText().trim());
                Integer.parseInt(stockField.getText().trim());
                Integer.parseInt(minStockField.getText().trim());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please check numeric fields."); return;
            }
            confirmed = true;
            dispose();
        }

        public boolean isConfirmed()       { return confirmed; }
        public String getItemId()          { return itemIdField.getText().trim(); }
        public String getDescriptionText() { return descField.getText().trim(); }
        public String getPackageType()     { return pkgField.getText().trim(); }
        public int    getUnitsInPack()     { return Integer.parseInt(unitsField.getText().trim()); }
        public double getPrice()           { return Double.parseDouble(priceField.getText().trim()); }
        public double getVatRate()         { return Double.parseDouble(vatField.getText().trim()); }
        public int    getStockQuantity()   { return Integer.parseInt(stockField.getText().trim()); }
        public int    getMinimumStock()    { return Integer.parseInt(minStockField.getText().trim()); }
    }
}
