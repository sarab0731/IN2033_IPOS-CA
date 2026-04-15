package ui;

import app.Session;
import database.MerchantDB;
import database.ProductDB;
import database.RestockOrderDB;
import domain.Merchant;
import domain.Product;
import domain.SACatalogueItem;
import integration.SASync;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.*;
import java.util.List;

public class StockPanel extends JPanel implements ThemeManager.ThemeListener {

    private final ScreenRouter router;

    // ── Tab bar ──────────────────────────────────────────────────────────────
    private JButton localStockTab;
    private JButton saCatalogueTab;
    private JPanel  viewCards;
    private CardLayout viewCardLayout;
    private static final String CARD_LOCAL = "local";
    private static final String CARD_SA    = "sa";

    // ── Local Stock components ───────────────────────────────────────────────
    private JPanel contentPanel;
    private JPanel tableCard;
    private JPanel topToolbar;
    private JPanel bottomActionBar;
    private JPanel footerPanel;
    private JLabel warningLabel;
    private JTable table;
    private JScrollPane scrollPane;
    private JButton editBtn;
    private JButton deleteBtn;
    private JButton restockBtn;
    private JButton refreshBtn;
    private JComboBox<String> filterCombo;
    private JComboBox<String> sortCombo;
    private DefaultTableModel tableModel;
    private final List<Product> allProducts     = new ArrayList<>();
    private final List<Product> visibleProducts = new ArrayList<>();
    private int currentPage = 1;
    private static final int PAGE_SIZE = 15;

    // ── SA Catalogue components ──────────────────────────────────────────────
    private JTable saTable;
    private JScrollPane saScrollPane;
    private DefaultTableModel saTableModel;
    private JTable saCartTable;
    private JScrollPane saCartScrollPane;
    private DefaultTableModel saCartModel;
    private JTextField saSearchField;
    private JLabel merchantIdLabel;
    private JLabel saCartTotalLabel;
    private JButton saAddToCartBtn;
    private JButton saRemoveBtn;
    private JButton saClearBtn;
    private JButton saPlaceOrderBtn;
    private final List<SACatalogueItem> saCatalogueAll     = new ArrayList<>();
    private final List<SACatalogueItem> saCatalogueVisible = new ArrayList<>();
    private final Map<SACatalogueItem, Integer> saCart      = new LinkedHashMap<>();

    // ── Constructor ──────────────────────────────────────────────────────────

    public StockPanel(ScreenRouter router) {
        this.router = router;
        setLayout(new BorderLayout());
        ThemeManager.register(this);

        AppShell shell = new AppShell(
                router,
                MainFrame.SCREEN_STOCK,
                "Stock Information",
                "Manage local stock and order from InfoPharma SA",
                buildContent()
        );

        add(shell, BorderLayout.CENTER);
        wireActions();
        loadTable();
        loadSACatalogue();
        applyTheme();
    }

    // ── Top-level content ────────────────────────────────────────────────────

    private JPanel buildContent() {
        contentPanel = new JPanel(new BorderLayout(0, 10));
        contentPanel.setBorder(new EmptyBorder(6, 6, 6, 6));
        contentPanel.setOpaque(true);

        contentPanel.add(buildTabBar(), BorderLayout.NORTH);

        viewCardLayout = new CardLayout();
        viewCards = new JPanel(viewCardLayout);
        viewCards.setOpaque(false);
        viewCards.add(buildLocalStockPanel(), CARD_LOCAL);
        viewCards.add(buildSACataloguePanel(), CARD_SA);
        contentPanel.add(viewCards, BorderLayout.CENTER);

        warningLabel = new JLabel(" ");
        warningLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        warningLabel.setBorder(new EmptyBorder(0, 4, 0, 0));
        contentPanel.add(warningLabel, BorderLayout.SOUTH);

        return contentPanel;
    }

    private JPanel buildTabBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        bar.setOpaque(false);
        localStockTab  = createTabButton("Local Stock",  true);
        saCatalogueTab = createTabButton("Order Stock",  false);
        bar.add(localStockTab);
        bar.add(Box.createHorizontalStrut(8));
        bar.add(saCatalogueTab);
        return bar;
    }

    // ── Local Stock panel ────────────────────────────────────────────────────

    private JPanel buildLocalStockPanel() {
        JPanel panel = new JPanel(new BorderLayout(16, 16));
        panel.setOpaque(false);

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
        southWrapper.add(footerPanel,      BorderLayout.NORTH);
        southWrapper.add(bottomActionBar,  BorderLayout.SOUTH);
        tableCard.add(southWrapper, BorderLayout.SOUTH);

        panel.add(tableCard, BorderLayout.CENTER);
        return panel;
    }

    private void buildToolbar() {
        topToolbar = new JPanel(new BorderLayout());
        topToolbar.setOpaque(false);

        filterCombo = new JComboBox<>(new String[]{"All Stocks", "Good", "Low Stock", "Restock"});
        sortCombo   = new JComboBox<>(new String[]{
                "Sort by: Quantity", "Sort by: Stock ID", "Sort by: Price", "Sort by: Description"
        });

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        right.add(filterCombo);
        right.add(sortCombo);

        topToolbar.add(right, BorderLayout.EAST);
    }

    private void buildTable() {
        tableModel = new DefaultTableModel(
                new String[]{"Stock ID", "Description", "Stock Quantity", "Price", "Rate of Vat", "Status"}, 0
        ) { @Override public boolean isCellEditable(int r, int c) { return false; } };
        table = new JTable(tableModel);
        configureTable(table);
        scrollPane = new JScrollPane(table);
        styleScrollPane(scrollPane);
    }

    private void buildFooter() {
        footerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        footerPanel.setOpaque(false);
        // Chips are built dynamically by refreshFooter() after each table refresh
    }

    private void buildBottomActionBar() {
        bottomActionBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        bottomActionBar.setOpaque(false);
        editBtn    = createPillButton("Edit Product",   false);
        deleteBtn  = createPillButton("Remove Product", false);
        restockBtn = createPillButton("Restock",        false);
        refreshBtn = createPillButton("Refresh",        false);
        bottomActionBar.add(editBtn);
        bottomActionBar.add(deleteBtn);
        bottomActionBar.add(restockBtn);
        bottomActionBar.add(refreshBtn);
    }

    // ── SA Catalogue panel ───────────────────────────────────────────────────

    private JPanel buildSACataloguePanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setOpaque(false);

        // Controls
        JPanel controls = new JPanel(new BorderLayout(12, 0));
        controls.setOpaque(false);

        JPanel leftControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftControls.setOpaque(false);
        saSearchField = new JTextField("Search catalogue...");
        saSearchField.setPreferredSize(new Dimension(260, 36));
        saSearchField.setFont(new Font("SansSerif", Font.PLAIN, 13));
        saSearchField.setForeground(Color.GRAY);
        leftControls.add(new JLabel("Search:"));
        leftControls.add(saSearchField);

        JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightControls.setOpaque(false);
        String mid = Session.hasMerchant() ? Session.getMerchant().getDisplayName() : "Not configured";
        merchantIdLabel = new JLabel(mid);
        merchantIdLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        rightControls.add(new JLabel("Merchant ID:"));
        rightControls.add(merchantIdLabel);

        controls.add(leftControls,  BorderLayout.WEST);
        controls.add(rightControls, BorderLayout.EAST);

        // Split: catalogue | cart
        JPanel split = new JPanel(new GridLayout(1, 2, 16, 0));
        split.setOpaque(false);

        // Left card — SA catalogue
        JPanel catalogueCard = AppShell.createCard();
        catalogueCard.setLayout(new BorderLayout(8, 8));
        catalogueCard.setBorder(new EmptyBorder(16, 16, 16, 16));
        JLabel catLabel = new JLabel("InfoPharma SA Catalogue");
        catLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        saTableModel = new DefaultTableModel(
                new String[]{"Item ID", "Description", "Unit Cost £", "Available (packs)"}, 0
        ) { @Override public boolean isCellEditable(int r, int c) { return false; } };
        saTable = new JTable(saTableModel);
        configureSATable(saTable);
        saScrollPane = new JScrollPane(saTable);
        styleSAScrollPane(saScrollPane);
        saAddToCartBtn = createPillButton("+ Add to Order", true);
        JPanel catBottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        catBottom.setOpaque(false);
        catBottom.add(saAddToCartBtn);
        catalogueCard.add(catLabel,    BorderLayout.NORTH);
        catalogueCard.add(saScrollPane, BorderLayout.CENTER);
        catalogueCard.add(catBottom,   BorderLayout.SOUTH);

        // Right card — order cart
        JPanel cartCard = AppShell.createCard();
        cartCard.setLayout(new BorderLayout(8, 8));
        cartCard.setBorder(new EmptyBorder(16, 16, 16, 16));
        JLabel cartLabel = new JLabel("Order Summary");
        cartLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        saCartModel = new DefaultTableModel(
                new String[]{"Item ID", "Description", "Qty", "Unit Cost £", "Line Total £"}, 0
        ) { @Override public boolean isCellEditable(int r, int c) { return false; } };
        saCartTable = new JTable(saCartModel);
        configureSATable(saCartTable);
        saCartScrollPane = new JScrollPane(saCartTable);
        styleSAScrollPane(saCartScrollPane);
        saCartTotalLabel = new JLabel("Total: £0.00");
        saCartTotalLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        JPanel totalWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        totalWrap.setOpaque(false);
        totalWrap.add(saCartTotalLabel);
        cartCard.add(cartLabel,        BorderLayout.NORTH);
        cartCard.add(saCartScrollPane, BorderLayout.CENTER);
        cartCard.add(totalWrap,        BorderLayout.SOUTH);

        split.add(catalogueCard);
        split.add(cartCard);

        // Bottom action bar
        JPanel actionBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionBar.setOpaque(false);
        saRemoveBtn     = createPillButton("Remove Item",  false);
        saClearBtn      = createPillButton("Clear Order",  false);
        saPlaceOrderBtn = createPillButton("Place Order",  true);
        actionBar.add(saRemoveBtn);
        actionBar.add(saClearBtn);
        actionBar.add(saPlaceOrderBtn);

        panel.add(controls,  BorderLayout.NORTH);
        panel.add(split,     BorderLayout.CENTER);
        panel.add(actionBar, BorderLayout.SOUTH);
        return panel;
    }

    // ── Wiring ───────────────────────────────────────────────────────────────

    private void wireActions() {
        // Tab switching
        localStockTab.addActionListener(e  -> switchTab(CARD_LOCAL));
        saCatalogueTab.addActionListener(e -> switchTab(CARD_SA));

        // Local stock
        refreshBtn.addActionListener(e -> loadTable());

        editBtn.addActionListener(e -> {
            int idx = getSelectedVisibleIndex();
            if (idx == -1) { JOptionPane.showMessageDialog(this, "Please select a product to edit."); return; }
            showEditDialog(visibleProducts.get(idx));
        });

        deleteBtn.addActionListener(e -> {
            int idx = getSelectedVisibleIndex();
            if (idx == -1) { JOptionPane.showMessageDialog(this, "Please select a product to remove."); return; }
            int ok = JOptionPane.showConfirmDialog(this, "Remove the selected product from stock?",
                    "Confirm removal", JOptionPane.YES_NO_OPTION);
            if (ok == JOptionPane.YES_OPTION) { ProductDB.deleteProduct(visibleProducts.get(idx).getProductId()); loadTable(); }
        });

        restockBtn.addActionListener(e -> {
            int idx = getSelectedVisibleIndex();
            if (idx == -1) { JOptionPane.showMessageDialog(this, "Please select a product to restock."); return; }
            String input = JOptionPane.showInputDialog(this, "Quantity to add:");
            if (input == null || input.trim().isEmpty()) return;
            try {
                int qty = Integer.parseInt(input.trim());
                if (qty <= 0) throw new NumberFormatException();
                ProductDB.updateStock(visibleProducts.get(idx).getProductId(), qty);
                loadTable();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter a valid positive quantity.");
            }
        });

        filterCombo.addActionListener(e -> { currentPage = 1; refreshTableView(); });
        sortCombo.addActionListener(e ->   { currentPage = 1; refreshTableView(); });

        // SA Catalogue
        saSearchField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) {
                if ("Search catalogue...".equals(saSearchField.getText())) {
                    saSearchField.setText(""); saSearchField.setForeground(ThemeManager.textPrimary());
                }
            }
            @Override public void focusLost(java.awt.event.FocusEvent e) {
                if (saSearchField.getText().trim().isEmpty()) {
                    saSearchField.setText("Search catalogue..."); saSearchField.setForeground(Color.GRAY);
                }
            }
        });
        saSearchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { filterSACatalogue(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { filterSACatalogue(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filterSACatalogue(); }
        });

        saAddToCartBtn.addActionListener(e  -> addSAItemToCart());
        saRemoveBtn.addActionListener(e     -> removeSACartItem());
        saClearBtn.addActionListener(e      -> { saCart.clear(); refreshSACart(); });
        saPlaceOrderBtn.addActionListener(e -> placeSAOrder());

        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentShown(java.awt.event.ComponentEvent e) { loadTable(); }
        });
    }

    private void switchTab(String card) {
        viewCardLayout.show(viewCards, card);
        boolean local = CARD_LOCAL.equals(card);
        localStockTab.setBackground(local  ? ThemeManager.buttonDark()  : ThemeManager.buttonLight());
        localStockTab.setForeground(local  ? ThemeManager.textLight()   : ThemeManager.textPrimary());
        saCatalogueTab.setBackground(!local ? ThemeManager.buttonDark() : ThemeManager.buttonLight());
        saCatalogueTab.setForeground(!local ? ThemeManager.textLight()  : ThemeManager.textPrimary());
        updateBanner(card);
    }

    /** Updates the footer banner to reflect the active tab's context. */
    private void updateBanner(String card) {
        if (warningLabel == null) return;
        if (CARD_SA.equals(card)) {
            if (!Session.hasMerchant()) {
                warningLabel.setForeground(Color.ORANGE.darker());
                warningLabel.setText("  SA merchant account not configured. Contact InfoPharma to register.");
                return;
            }
            Merchant m = Session.getMerchant();
            if (m.isAccountNormal()) {
                warningLabel.setForeground(ThemeManager.textPrimary());
                warningLabel.setText(String.format(
                        "  SA Account: %s  |  Balance: £%.2f  |  Credit Limit: £%.2f  |  Available: £%.2f",
                        m.getMerchantId(), m.getSaBalance(), m.getSaCreditLimit(), m.getAvailableCredit()));
            } else {
                warningLabel.setForeground(Color.RED);
                warningLabel.setText(String.format(
                        "  WARNING: SA account %s. Balance: £%.2f. Orders are blocked until the account is restored.",
                        m.getSaAccountStatus(), m.getSaBalance()));
            }
        } else {
            // Local stock tab — reuse the low-stock message already set by loadTable()
            int low = ProductDB.getLowStockCount();
            warningLabel.setForeground(low > 0 ? Color.ORANGE.darker() : ThemeManager.textPrimary());
            warningLabel.setText(low > 0
                    ? "  Warning: " + low + " product(s) are at or below minimum stock."
                    : "  All products are above minimum stock.");
        }
    }

    // ── SA Catalogue logic ───────────────────────────────────────────────────

    private void loadSACatalogue() {
        saCatalogueAll.clear();
        // Load from the local SA catalogue cache (populated by SASync at startup).
        // If SA was offline at startup the table will be empty until next sync.
        saCatalogueAll.addAll(MerchantDB.getCatalogueItems());
        filterSACatalogue();
    }

    private void filterSACatalogue() {
        String kw = saSearchField != null ? saSearchField.getText().trim().toLowerCase() : "";
        if (kw.isEmpty() || kw.equals("search catalogue...")) {
            populateSATable(saCatalogueAll);
        } else {
            List<SACatalogueItem> filtered = new ArrayList<>();
            for (SACatalogueItem i : saCatalogueAll)
                if (i.getDescription().toLowerCase().contains(kw) || i.getItemId().toLowerCase().contains(kw))
                    filtered.add(i);
            populateSATable(filtered);
        }
    }

    private void populateSATable(List<SACatalogueItem> items) {
        saCatalogueVisible.clear();
        saCatalogueVisible.addAll(items);
        saTableModel.setRowCount(0);
        for (SACatalogueItem i : items)
            saTableModel.addRow(new Object[]{
                    i.getItemId(),
                    i.getDescription(),
                    String.format("£%.2f", i.getUnitCost()),
                    i.getAvailability()
            });
    }

    private void addSAItemToCart() {
        int row = saTable.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this, "Select an item from the catalogue."); return; }
        SACatalogueItem item = saCatalogueVisible.get(saTable.convertRowIndexToModel(row));
        String input = JOptionPane.showInputDialog(this, "Quantity to order:", "1");
        if (input == null || input.trim().isEmpty()) return;
        try {
            int qty = Integer.parseInt(input.trim());
            if (qty <= 0) throw new NumberFormatException();
            saCart.merge(item, qty, Integer::sum);
            refreshSACart();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Enter a valid positive quantity.");
        }
    }

    private void removeSACartItem() {
        int row = saCartTable.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this, "Select an item to remove."); return; }
        SACatalogueItem key = (SACatalogueItem) saCart.keySet().toArray()[saCartTable.convertRowIndexToModel(row)];
        saCart.remove(key);
        refreshSACart();
    }

    private void refreshSACart() {
        saCartModel.setRowCount(0);
        double total = 0;
        for (Map.Entry<SACatalogueItem, Integer> e : saCart.entrySet()) {
            double line = e.getKey().getUnitCost() * e.getValue();
            total += line;
            saCartModel.addRow(new Object[]{
                    e.getKey().getItemId(), e.getKey().getDescription(), e.getValue(),
                    String.format("%.2f", e.getKey().getUnitCost()),
                    String.format("%.2f", line)
            });
        }
        if (saCartTotalLabel != null) saCartTotalLabel.setText("Total: £" + String.format("%.2f", total));
    }

    private void placeSAOrder() {
        if (!Session.isManagerOrAdmin()) {
            JOptionPane.showMessageDialog(this, "Only Managers and Admins can place orders.");
            return;
        }
        if (saCart.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Add items to the order first.");
            return;
        }
        if (!Session.hasMerchant()) {
            JOptionPane.showMessageDialog(this,
                    "No SA merchant account configured.\nPlease contact your InfoPharma SA administrator.",
                    "SA Not Configured", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Merchant merchant = Session.getMerchant();
        if (!merchant.isAccountNormal()) {
            JOptionPane.showMessageDialog(this,
                    "Cannot place order.\nSA account status: " + merchant.getSaAccountStatus()
                    + "\nPlease settle the outstanding balance first.",
                    "Account Restricted", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String merchantId = merchant.getMerchantId();

        // Resolve real product_ids — SA items are upserted into products (stock=0)
        // so the restock_order_items FK is satisfied, and stock increments on delivery.
        Map<Product, Integer> productCart = new LinkedHashMap<>();
        double orderTotal = 0;
        for (Map.Entry<SACatalogueItem, Integer> e : saCart.entrySet()) {
            SACatalogueItem sa  = e.getKey();
            int             qty = e.getValue();
            int productId = MerchantDB.ensureProduct(sa);
            if (productId == -1) {
                JOptionPane.showMessageDialog(this,
                        "Failed to register product " + sa.getItemId() + ". Order aborted.");
                return;
            }
            productCart.put(new Product(productId, sa.getItemId(), sa.getDescription(),
                    "Pack of " + sa.getPackSize(), sa.getPackSize(),
                    sa.getUnitCost(), 0.0, 0, 0), qty);
            orderTotal += sa.getUnitCost() * qty;
        }

        String orderNumber = RestockOrderDB.placeOrder(merchantId, productCart);
        if (orderNumber == null) {
            JOptionPane.showMessageDialog(this, "Failed to place order. Please try again.");
            return;
        }

        // Notify SA
        JSONArray itemsJson = new JSONArray();
        for (Map.Entry<SACatalogueItem, Integer> e : saCart.entrySet())
            itemsJson.put(new JSONObject().put("itemId", e.getKey().getItemId()).put("quantity", e.getValue()));
        String saOrderId = SASync.placeOrderViaSA(merchantId,
                new JSONObject().put("items", itemsJson).toString());
        if (saOrderId != null) RestockOrderDB.updateSAOrderId(orderNumber, saOrderId);

        saCart.clear();
        refreshSACart();

        int nav = JOptionPane.showConfirmDialog(this,
                "Order placed successfully!\nLocal Order: " + orderNumber
                + (saOrderId != null ? "\nSA Order ID: " + saOrderId : "\n(SA offline — queued locally)")
                + "\n\nGo to Order Management to view it?",
                "Order Placed", JOptionPane.YES_NO_OPTION);
        if (nav == JOptionPane.YES_OPTION) router.goTo(MainFrame.SCREEN_ORDERS);
    }

    // ── Local stock loading ──────────────────────────────────────────────────

    private void loadTable() {
        allProducts.clear();
        allProducts.addAll(ProductDB.getAllProducts());
        refreshTableView();
        int low = ProductDB.getLowStockCount();
        if (warningLabel != null) {
            warningLabel.setForeground(low > 0 ? Color.ORANGE.darker() : ThemeManager.textPrimary());
            warningLabel.setText(low > 0
                    ? "  Warning: " + low + " product(s) are at or below minimum stock."
                    : "  All products are above minimum stock.");
        }
    }

    public void refreshTableView() {
        tableModel.setRowCount(0);
        visibleProducts.clear();

        String filter = filterCombo != null ? (String) filterCombo.getSelectedItem() : "All Stocks";
        String sort   = sortCombo   != null ? (String) sortCombo.getSelectedItem()   : "Sort by: Quantity";

        List<Product> filtered = new ArrayList<>();
        for (Product p : allProducts) {
            String status = getStatusText(p);
            if ("All Stocks".equals(filter) || status.equalsIgnoreCase(filter)) filtered.add(p);
        }

        switch (sort) {
            case "Sort by: Quantity"    -> filtered.sort(Comparator.comparingInt(Product::getStockQuantity));
            case "Sort by: Stock ID"    -> filtered.sort(Comparator.comparingInt(Product::getProductId));
            case "Sort by: Price"       -> filtered.sort(Comparator.comparingDouble(Product::getPrice));
            case "Sort by: Description" -> filtered.sort(Comparator.comparing(Product::getDescription, String.CASE_INSENSITIVE_ORDER));
        }

        // Pagination
        int totalPages = Math.max(1, (int) Math.ceil((double) filtered.size() / PAGE_SIZE));
        currentPage = Math.max(1, Math.min(currentPage, totalPages));
        int from = (currentPage - 1) * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, filtered.size());

        visibleProducts.addAll(filtered.subList(from, to));
        for (Product p : visibleProducts)
            tableModel.addRow(new Object[]{
                    String.format("%03d", p.getProductId()),
                    p.getDescription(),
                    p.getStockQuantity(),
                    String.format("£%.2f", p.getPrice()),
                    String.format("%.1f%%", p.getVatRate()),
                    getStatusText(p)
            });

        refreshFooter(totalPages);
        applyTheme();
    }

    private void refreshFooter(int totalPages) {
        if (footerPanel == null) return;
        footerPanel.removeAll();

        // Prev arrow
        JButton prev = createPageChip("‹", false);
        prev.setEnabled(currentPage > 1);
        if (currentPage > 1) prev.addActionListener(e -> { currentPage--; refreshTableView(); });
        footerPanel.add(prev);

        // Page window: up to 5 pages centred on currentPage
        int start = Math.max(1, currentPage - 2);
        int end   = Math.min(totalPages, start + 4);
        start     = Math.max(1, end - 4);

        if (start > 1) {
            footerPanel.add(createPageChipWithAction("1", 1 == currentPage, 1));
            if (start > 2) { JButton dots = createPageChip("…", false); dots.setEnabled(false); footerPanel.add(dots); }
        }
        for (int i = start; i <= end; i++)
            footerPanel.add(createPageChipWithAction(String.valueOf(i), i == currentPage, i));
        if (end < totalPages) {
            if (end < totalPages - 1) { JButton dots = createPageChip("…", false); dots.setEnabled(false); footerPanel.add(dots); }
            footerPanel.add(createPageChipWithAction(String.valueOf(totalPages), totalPages == currentPage, totalPages));
        }

        // Next arrow
        JButton next = createPageChip("›", false);
        next.setEnabled(currentPage < totalPages);
        if (currentPage < totalPages) next.addActionListener(e -> { currentPage++; refreshTableView(); });
        footerPanel.add(next);

        footerPanel.revalidate();
        footerPanel.repaint();
    }

    private JButton createPageChipWithAction(String text, boolean active, int page) {
        JButton btn = createPageChip(text, active);
        if (!active) btn.addActionListener(e -> { currentPage = page; refreshTableView(); });
        return btn;
    }

    private String getStatusText(Product p) {
        int stock = p.getStockQuantity(), min = p.getMinStockLevel();
        if (stock == 0) return "Restock";
        if (stock <= min) return "Low Stock";
        return "Good";
    }

    private int getSelectedVisibleIndex() {
        int row = table.getSelectedRow();
        return row == -1 ? -1 : table.convertRowIndexToModel(row);
    }

    // ── Dialogs ──────────────────────────────────────────────────────────────

    private void showEditDialog(Product product) {
        Product fresh = ProductDB.getById(product.getProductId());
        if (fresh == null) { JOptionPane.showMessageDialog(this, "Could not load the selected product."); return; }
        ProductFormDialog dlg = new ProductFormDialog(
                SwingUtilities.getWindowAncestor(this), "Edit Product", false, fresh);
        dlg.setVisible(true);
        if (!dlg.isConfirmed()) return;
        fresh.setDescription(dlg.getDescriptionText());
        fresh.setPackageType(dlg.getPackageType());
        fresh.setUnitsInPack(dlg.getUnitsInPack());
        fresh.setPrice(dlg.getPrice());
        fresh.setVatRate(dlg.getVatRate());
        fresh.setStockQuantity(dlg.getStockQuantity());
        fresh.setMinStockLevel(dlg.getMinimumStock());
        if (ProductDB.updateProduct(fresh)) loadTable();
        else JOptionPane.showMessageDialog(this, "Could not update the product.");
    }

    // ── Table configuration ──────────────────────────────────────────────────

    private void configureTable(JTable t) {
        t.setRowHeight(42); t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setFillsViewportHeight(true);
        t.setBorder(BorderFactory.createEmptyBorder());
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setDefaultEditor(Object.class, null);
        t.setFont(new Font("SansSerif", Font.PLAIN, 13));
        t.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        JTableHeader h = t.getTableHeader();
        h.setReorderingAllowed(false); h.setResizingAllowed(false);
        h.setFont(new Font("SansSerif", Font.BOLD, 13));
        h.setBorder(BorderFactory.createEmptyBorder());
        h.setPreferredSize(new Dimension(h.getPreferredSize().width, 40));
        DefaultTableCellRenderer base = new DefaultTableCellRenderer();
        base.setBorder(new EmptyBorder(0, 10, 0, 10));
        base.setHorizontalAlignment(SwingConstants.LEFT);
        for (int i = 0; i < t.getColumnCount() - 1; i++) t.getColumnModel().getColumn(i).setCellRenderer(base);
        t.getColumnModel().getColumn(5).setCellRenderer(new StatusCellRenderer());
    }

    private void configureSATable(JTable t) {
        t.setRowHeight(38); t.setShowGrid(true);
        t.setIntercellSpacing(new Dimension(1, 1));
        t.setFillsViewportHeight(true);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setDefaultEditor(Object.class, null);
        t.setFont(new Font("SansSerif", Font.PLAIN, 13));
        JTableHeader h = t.getTableHeader();
        h.setReorderingAllowed(false);
        h.setFont(new Font("SansSerif", Font.BOLD, 13));
        h.setBorder(BorderFactory.createEmptyBorder());
        DefaultTableCellRenderer r = new DefaultTableCellRenderer();
        r.setHorizontalAlignment(SwingConstants.LEFT);
        r.setBorder(new EmptyBorder(0, 10, 0, 10));
        t.setDefaultRenderer(Object.class, r);
    }

    // ── Scroll pane styling ──────────────────────────────────────────────────

    private void styleScrollPane(JScrollPane sp) {
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setOpaque(true);
        sp.setBackground(ThemeManager.tableBackground());
        JViewport vp = sp.getViewport();
        vp.setBackground(ThemeManager.tableBackground());
        vp.setBorder(null); vp.setOpaque(true);
    }

    private void styleSAScrollPane(JScrollPane sp) {
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setBackground(ThemeManager.tableBackground());
        sp.getViewport().setBackground(ThemeManager.tableBackground());
        sp.getViewport().setBorder(null);
    }

    // ── Theme ────────────────────────────────────────────────────────────────

    @Override
    public void applyTheme() {
        setBackground(ThemeManager.appBackground());
        if (contentPanel    != null) contentPanel.setBackground(ThemeManager.appBackground());
        if (tableCard       != null) tableCard.setBackground(ThemeManager.panelBackground());
        if (topToolbar      != null) topToolbar.setBackground(ThemeManager.panelBackground());
        if (bottomActionBar != null) bottomActionBar.setBackground(ThemeManager.panelBackground());
        if (footerPanel     != null) footerPanel.setBackground(ThemeManager.panelBackground());
        if (warningLabel    != null) warningLabel.setForeground(ProductDB.getLowStockCount() > 0
                ? new Color(190, 76, 76) : ThemeManager.textSecondary());

        if (editBtn    != null) stylePillButton(editBtn,    false);
        if (deleteBtn  != null) stylePillButton(deleteBtn,  false);
        if (restockBtn != null) stylePillButton(restockBtn, false);
        if (refreshBtn != null) stylePillButton(refreshBtn, false);
        if (filterCombo != null) styleComboBox(filterCombo);
        if (sortCombo   != null) styleComboBox(sortCombo);
        if (scrollPane  != null) styleScrollPane(scrollPane);
        applyTableTheme();

        if (saAddToCartBtn  != null) stylePillButton(saAddToCartBtn,  true);
        if (saRemoveBtn     != null) stylePillButton(saRemoveBtn,     false);
        if (saClearBtn      != null) stylePillButton(saClearBtn,      false);
        if (saPlaceOrderBtn != null) stylePillButton(saPlaceOrderBtn, true);
        if (saScrollPane     != null) styleSAScrollPane(saScrollPane);
        if (saCartScrollPane != null) styleSAScrollPane(saCartScrollPane);

        repaint(); revalidate();
    }

    private void applyTableTheme() {
        if (table == null) return;
        table.setBackground(ThemeManager.tableBackground());
        table.setForeground(ThemeManager.textPrimary());
        table.setSelectionBackground(ThemeManager.selectionBackground());
        table.setSelectionForeground(ThemeManager.textPrimary());
        JTableHeader h = table.getTableHeader();
        if (h != null) {
            h.setBackground(ThemeManager.tableBackground());
            h.setForeground(ThemeManager.textPrimary());
            h.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ThemeManager.borderColor()));
            h.setOpaque(true);
        }
        table.repaint();
    }

    // ── Button / combo factories ─────────────────────────────────────────────

    private JButton createPillButton(String text, boolean primary) {
        JButton btn = new JButton(text);
        btn.setFocusPainted(false); btn.setOpaque(true); btn.setContentAreaFilled(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.putClientProperty("primary", primary);
        stylePillButton(btn, primary);
        return btn;
    }

    private JButton createTabButton(String text, boolean active) {
        JButton btn = new JButton(text);
        btn.setFocusPainted(false); btn.setOpaque(true); btn.setContentAreaFilled(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setFont(new Font("SansSerif", Font.BOLD, 14));
        btn.setBorder(new EmptyBorder(10, 20, 10, 20));
        btn.setBackground(active ? ThemeManager.buttonDark() : ThemeManager.buttonLight());
        btn.setForeground(active ? ThemeManager.textLight()  : ThemeManager.textPrimary());
        return btn;
    }

    private JButton createPageChip(String text, boolean active) {
        JButton btn = new JButton(text);
        btn.setFocusPainted(false); btn.setOpaque(true); btn.setContentAreaFilled(true);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setPreferredSize(new Dimension(26, 24));
        btn.setBorder(BorderFactory.createEmptyBorder());
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBackground(active ? new Color(77, 77, 77)
                : (ThemeManager.isDark() ? new Color(55, 58, 66) : new Color(245, 245, 245)));
        btn.setForeground(active ? Color.WHITE : ThemeManager.textSecondary());
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
                    new EmptyBorder(9, 16, 9, 16)));
        }
    }

    private void styleComboBox(JComboBox<String> cb) {
        cb.setFocusable(false);
        cb.setFont(new Font("SansSerif", Font.PLAIN, 13));
        cb.setPreferredSize(new Dimension(130, 30));
        cb.setBackground(ThemeManager.comboBackground());
        cb.setForeground(ThemeManager.comboForeground());
        cb.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.borderColor()),
                new EmptyBorder(4, 8, 4, 8)));
    }

    // ── Inner classes ────────────────────────────────────────────────────────

    private class StatusCellRenderer extends DefaultTableCellRenderer {
        public StatusCellRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
            setFont(new Font("SansSerif", Font.BOLD, 12));
            setBorder(new EmptyBorder(0, 10, 0, 10));
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String status = value == null ? "" : value.toString();
            setForeground(Color.BLACK);
            if ("Restock".equalsIgnoreCase(status))        setBackground(new Color(237, 106, 94));
            else if ("Low Stock".equalsIgnoreCase(status)) setBackground(new Color(244, 213, 96));
            else                                           setBackground(new Color(204, 227, 102));
            setBorder(isSelected
                    ? BorderFactory.createLineBorder(ThemeManager.isDark() ? new Color(220, 220, 220) : new Color(80, 80, 80), 1)
                    : new EmptyBorder(0, 10, 0, 10));
            setText(status);
            return this;
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
            g2.setFont(b.getFont()); g2.setColor(b.getForeground());
            g2.drawString(text, (c.getWidth() - fm.stringWidth(text)) / 2,
                    (c.getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    private static class ProductFormDialog extends JDialog {
        private boolean confirmed = false;
        private JTextField itemIdField, descField, pkgField, unitsField,
                           priceField, vatField, stockField, minStockField;

        ProductFormDialog(Window owner, String title, boolean addMode, Product product) {
            super(owner, title, ModalityType.APPLICATION_MODAL);
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            setResizable(false);
            setContentPane(buildUI(product, addMode));
            setSize(520, 780);
            setLocationRelativeTo(owner);
        }

        private JPanel buildUI(Product product, boolean addMode) {
            JPanel root = new JPanel(new GridBagLayout());
            root.setBackground(ThemeManager.appBackground());
            root.setBorder(new EmptyBorder(16, 16, 16, 16));

            JPanel card = new JPanel(new GridBagLayout());
            card.setBackground(ThemeManager.panelBackground());
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ThemeManager.borderColor()),
                    new EmptyBorder(20, 24, 20, 24)));

            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0; c.gridy = 0; c.weightx = 1.0;
            c.fill = GridBagConstraints.HORIZONTAL;

            JLabel titleLabel = new JLabel(addMode ? "Order / Add Stock" : "Edit Product");
            titleLabel.setFont(new Font("SansSerif", Font.BOLD, 28));
            titleLabel.setForeground(ThemeManager.textPrimary());
            c.insets = new Insets(0, 0, 10, 0);
            card.add(titleLabel, c);

            c.gridy++;
            JLabel sub = new JLabel(addMode ? "Enter product details to add new stock."
                                            : "Update the product details below.");
            sub.setFont(new Font("SansSerif", Font.PLAIN, 14));
            sub.setForeground(ThemeManager.textSecondary());
            c.insets = new Insets(0, 0, 16, 0);
            card.add(sub, c);

            itemIdField  = createField();
            descField    = createField();
            pkgField     = createField();
            unitsField   = createField();
            priceField   = createField();
            vatField     = createField();
            stockField   = createField();
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
            addField(card, c, "Stock Quantity", stockField);
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
            if (itemIdField.getText().trim().isEmpty()) { JOptionPane.showMessageDialog(this, "Item ID is required."); return; }
            if (descField.getText().trim().isEmpty())   { JOptionPane.showMessageDialog(this, "Description is required."); return; }
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

        public boolean isConfirmed()      { return confirmed; }
        public String getItemId()         { return itemIdField.getText().trim(); }
        public String getDescriptionText(){ return descField.getText().trim(); }
        public String getPackageType()    { return pkgField.getText().trim(); }
        public int    getUnitsInPack()    { return Integer.parseInt(unitsField.getText().trim()); }
        public double getPrice()          { return Double.parseDouble(priceField.getText().trim()); }
        public double getVatRate()        { return Double.parseDouble(vatField.getText().trim()); }
        public int    getStockQuantity()  { return Integer.parseInt(stockField.getText().trim()); }
        public int    getMinimumStock()   { return Integer.parseInt(minStockField.getText().trim()); }
    }
}
