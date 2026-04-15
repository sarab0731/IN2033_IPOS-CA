package ui;

import app.Session;
import database.MerchantDB;
import database.RestockOrderDB;
import domain.Merchant;
import domain.Product;
import domain.RestockOrder;
import domain.RestockOrderItem;
import domain.SACatalogueItem;
import integration.SAApiClient;
import integration.SASync;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OrdersPanel extends JPanel implements ThemeManager.ThemeListener {

    private final ScreenRouter router;

    // ── Tab / card switching ──────────────────────────────────────────────────
    private JPanel      contentPanel;
    private JPanel      topTabBar;
    private CardLayout  orderCardLayout;
    private JPanel      orderCards;
    private JButton     placeNewOrderTab;
    private JButton     orderHistoryTab;
    private static final String CARD_NEW_ORDER = "newOrder";
    private static final String CARD_HISTORY   = "history";

    // ── New-order card components ─────────────────────────────────────────────
    private JPanel controlsPanel;
    private JPanel leftCard;
    private JPanel rightCard;
    private JPanel bottomBar;
    private JTable catalogueTable;
    private JTable summaryTable;
    private JScrollPane catalogueScrollPane;
    private JScrollPane summaryScrollPane;
    private JButton addToOrderBtn;
    private JButton removeItemBtn;
    private JButton clearOrderBtn;
    private JButton placeOrderBtn;
    private JLabel  availableProductsLabel;
    private JLabel  orderSummaryLabel;
    private JLabel  merchantIdLabel;
    private JLabel  merchantIdValue;
    private JLabel  totalLabel;
    private JTextField searchField;
    private DefaultTableModel catalogueModel;
    private DefaultTableModel summaryModel;

    // ── History card components ───────────────────────────────────────────────
    private JTable            historyTable;
    private DefaultTableModel historyModel;

    // ── Merchant status card ──────────────────────────────────────────────────
    private JLabel merchantStatusLabel;
    private JLabel activeOrdersValue;
    private JLabel outstandingValue;
    private JLabel accountStatusLabel;

    private final Map<SACatalogueItem, Integer> cart = new LinkedHashMap<>();
    private List<SACatalogueItem> currentCatalogue = new ArrayList<>();

    public OrdersPanel(ScreenRouter router) {
        this.router = router;
        setLayout(new BorderLayout());
        ThemeManager.register(this);

        AppShell shell = new AppShell(
                router, MainFrame.SCREEN_ORDERS,
                "Order Management",
                "Place and track restock orders with InfoPharma SA",
                buildContent()
        );

        add(shell, BorderLayout.CENTER);
        wireActions();
        loadCatalogue();
        applyTheme();
    }

    private JPanel buildContent() {
        contentPanel = new JPanel(new BorderLayout(20, 20));
        contentPanel.setBorder(new EmptyBorder(8, 8, 8, 8));
        contentPanel.setOpaque(true);

        // Tab bar
        topTabBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 18, 0));
        topTabBar.setOpaque(false);
        placeNewOrderTab = createBtn("Place New Order", true);
        orderHistoryTab  = createBtn("Order History", false);
        topTabBar.add(placeNewOrderTab);
        topTabBar.add(orderHistoryTab);

        JPanel merchantCard = buildMerchantStatusCard();

        JPanel northStack = new JPanel();
        northStack.setLayout(new BoxLayout(northStack, BoxLayout.Y_AXIS));
        northStack.setOpaque(false);
        northStack.add(topTabBar);
        northStack.add(Box.createVerticalStrut(8));
        northStack.add(merchantCard);

        // Main card area — switches between new-order form and order history
        orderCardLayout = new CardLayout();
        orderCards = new JPanel(orderCardLayout);
        orderCards.setOpaque(false);
        orderCards.add(buildNewOrderCard(), CARD_NEW_ORDER);
        orderCards.add(buildHistoryCard(),  CARD_HISTORY);

        contentPanel.add(northStack, BorderLayout.NORTH);
        contentPanel.add(orderCards, BorderLayout.CENTER);
        return contentPanel;
    }

    /** The "Place New Order" card — catalogue on the left, order summary on the right. */
    private JPanel buildNewOrderCard() {
        controlsPanel = new JPanel(new BorderLayout(20, 0));
        controlsPanel.setOpaque(false);

        JPanel leftButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftButtons.setOpaque(false);
        addToOrderBtn = createBtn("+  Add To Order", true);
        removeItemBtn = createBtn("Remove Item", false);
        searchField = new JTextField("Search catalogue...");
        searchField.setPreferredSize(new Dimension(200, 36));
        searchField.setFont(new Font("SansSerif", Font.PLAIN, 13));
        searchField.setForeground(Color.GRAY);
        leftButtons.add(addToOrderBtn);
        leftButtons.add(removeItemBtn);
        leftButtons.add(searchField);

        JPanel rightMerchant = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        rightMerchant.setOpaque(false);
        merchantIdLabel = new JLabel("Merchant ID:");
        merchantIdValue = new JLabel(Session.hasMerchant() ? Session.getMerchant().getDisplayName() : "Not configured");
        merchantIdValue.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.borderColor()),
                new EmptyBorder(6, 12, 6, 12)));
        rightMerchant.add(merchantIdLabel);
        rightMerchant.add(merchantIdValue);
        controlsPanel.add(leftButtons, BorderLayout.WEST);
        controlsPanel.add(rightMerchant, BorderLayout.EAST);

        JPanel centerSection = new JPanel(new GridLayout(1, 2, 20, 0));
        centerSection.setOpaque(false);

        leftCard = AppShell.createCard();
        leftCard.setLayout(new BorderLayout(12, 12));
        availableProductsLabel = new JLabel("InfoPharma SA Catalogue");
        availableProductsLabel.setFont(new Font("SansSerif", Font.BOLD, 16));

        rightCard = AppShell.createCard();
        rightCard.setLayout(new BorderLayout(12, 12));
        orderSummaryLabel = new JLabel("Order Summary");
        orderSummaryLabel.setFont(new Font("SansSerif", Font.BOLD, 16));

        catalogueModel = new DefaultTableModel(
                new String[]{"Item ID", "Description", "Unit Cost £", "Available (packs)"}, 0
        ) { @Override public boolean isCellEditable(int r, int c) { return false; } };
        catalogueTable = new JTable(catalogueModel);

        summaryModel = new DefaultTableModel(
                new String[]{"Item ID", "Description", "Qty", "Unit Cost £", "Line Total £"}, 0
        ) { @Override public boolean isCellEditable(int r, int c) { return false; } };
        summaryTable = new JTable(summaryModel);

        configureTable(catalogueTable);
        configureTable(summaryTable);
        catalogueScrollPane = new JScrollPane(catalogueTable);
        summaryScrollPane   = new JScrollPane(summaryTable);
        styleScrollPane(catalogueScrollPane);
        styleScrollPane(summaryScrollPane);

        leftCard.add(availableProductsLabel, BorderLayout.NORTH);
        leftCard.add(catalogueScrollPane, BorderLayout.CENTER);

        JPanel rightInner = new JPanel(new BorderLayout(12, 12));
        rightInner.setOpaque(false);
        rightInner.add(orderSummaryLabel, BorderLayout.NORTH);
        rightInner.add(summaryScrollPane, BorderLayout.CENTER);

        totalLabel = new JLabel("Total: £0.00");
        totalLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        JPanel totalWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        totalWrap.setOpaque(false);
        totalWrap.add(totalLabel);
        rightInner.add(totalWrap, BorderLayout.SOUTH);
        rightCard.add(rightInner, BorderLayout.CENTER);

        centerSection.add(leftCard);
        centerSection.add(rightCard);

        bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        bottomBar.setOpaque(false);
        clearOrderBtn = createBtn("Clear Order", false);
        placeOrderBtn = createBtn("Place Order", true);
        bottomBar.add(clearOrderBtn);
        bottomBar.add(placeOrderBtn);

        JPanel card = new JPanel(new BorderLayout(20, 20));
        card.setOpaque(false);
        card.add(controlsPanel, BorderLayout.NORTH);
        card.add(centerSection, BorderLayout.CENTER);
        card.add(bottomBar,     BorderLayout.SOUTH);
        return card;
    }

    /** The "Order History" card — inline table with action buttons, no modal needed. */
    private JPanel buildHistoryCard() {
        historyModel = new DefaultTableModel(
                new String[]{"ID", "Order Number", "Merchant ID", "Status", "Total £", "SA Order ID", "Created At"}, 0
        ) { @Override public boolean isCellEditable(int r, int c) { return false; } };
        historyTable = new JTable(historyModel);
        configureTable(historyTable);
        applyTableTheme(historyTable);
        JScrollPane scroll = new JScrollPane(historyTable);
        styleScrollPane(scroll);

        JButton refreshBtn   = createBtn("Refresh",          false);
        JButton viewItemsBtn = createBtn("View Items",       false);
        JButton trackBtn     = createBtn("Track Delivery",   false);
        JButton printBtn     = createBtn("Print Form",       false);
        JButton invoiceBtn   = createBtn("View Invoice",     false);
        JButton statusBtn    = createBtn("Update Status",    true);

        refreshBtn.addActionListener(e -> loadOrderHistory(historyModel));

        viewItemsBtn.addActionListener(e -> {
            int row = historyTable.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(this, "Select an order."); return; }
            showOrderItemsDialog(SwingUtilities.getWindowAncestor(this),
                    (int) historyModel.getValueAt(historyTable.convertRowIndexToModel(row), 0));
        });

        trackBtn.addActionListener(e -> {
            int row = historyTable.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(this, "Select an order."); return; }
            int mRow = historyTable.convertRowIndexToModel(row);
            int selectedOrderId = (int) historyModel.getValueAt(mRow, 0);
            String saOrderId = RestockOrderDB.getSAOrderId(selectedOrderId);
            if (saOrderId == null || saOrderId.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "No SA order ID for this order.\nSA was offline when it was placed, or tracking is unavailable.");
                return;
            }
            String tracking = SAApiClient.trackDelivery(saOrderId);
            JOptionPane.showMessageDialog(this,
                    tracking.isEmpty() ? "No tracking information available from SA." : tracking,
                    "Delivery Tracking — " + saOrderId, JOptionPane.INFORMATION_MESSAGE);
            if (!tracking.isEmpty()) {
                String saStatus   = parseSAStatus(tracking);
                String localStatus = String.valueOf(historyModel.getValueAt(mRow, 3));
                String mappedStatus = mapSAStatusToCA(saStatus);
                if (mappedStatus != null && isStatusProgression(localStatus, mappedStatus)) {
                    int sync = JOptionPane.showConfirmDialog(this,
                            "SA shows this order as: " + saStatus.toUpperCase()
                            + "\nLocal status:         " + localStatus
                            + "\n\nUpdate local order to " + mappedStatus + "?",
                            "Sync Status from SA", JOptionPane.YES_NO_OPTION);
                    if (sync == JOptionPane.YES_OPTION) {
                        RestockOrderDB.updateStatus(selectedOrderId, localStatus, mappedStatus);
                        if ("DELIVERED".equals(mappedStatus)) {
                            List<RestockOrderItem> delivered = RestockOrderDB.getOrderItems(selectedOrderId);
                            for (RestockOrderItem item : delivered)
                                SASync.deductSAStock(item.getItemId(), item.getQuantity());
                            refreshMerchantStatus();
                        }
                        loadOrderHistory(historyModel);
                    }
                }
            }
        });

        printBtn.addActionListener(e -> {
            int row = historyTable.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(this, "Select an order."); return; }
            int mRow = historyTable.convertRowIndexToModel(row);
            String num = String.valueOf(historyModel.getValueAt(mRow, 1));
            RestockOrder order = RestockOrderDB.getByOrderNumber(num);
            if (order == null) return;
            PdfGenerator.generateOrderForm(this, order,
                    RestockOrderDB.getOrderItems((int) historyModel.getValueAt(mRow, 0)));
        });

        invoiceBtn.addActionListener(e -> {
            int row = historyTable.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(this, "Select an order."); return; }
            int mRow = historyTable.convertRowIndexToModel(row);
            int selectedOrderId = (int) historyModel.getValueAt(mRow, 0);
            String saOrderId = RestockOrderDB.getSAOrderId(selectedOrderId);
            if (saOrderId != null && !saOrderId.isEmpty()) {
                String saInvoice = SAApiClient.getInvoice(saOrderId);
                if (saInvoice != null && !saInvoice.isEmpty()) {
                    JTextArea area = new JTextArea(saInvoice);
                    area.setEditable(false);
                    area.setFont(new Font("Monospaced", Font.PLAIN, 13));
                    area.setMargin(new Insets(12, 12, 12, 12));
                    JOptionPane.showMessageDialog(this, new JScrollPane(area),
                            "SA Invoice — " + saOrderId, JOptionPane.PLAIN_MESSAGE);
                    return;
                }
            }
            showOrderInvoiceDialog(SwingUtilities.getWindowAncestor(this),
                    String.valueOf(historyModel.getValueAt(mRow, 1)),
                    String.valueOf(historyModel.getValueAt(mRow, 2)),
                    String.valueOf(historyModel.getValueAt(mRow, 3)),
                    String.valueOf(historyModel.getValueAt(mRow, 4)),
                    String.valueOf(historyModel.getValueAt(mRow, 6)));
        });

        statusBtn.addActionListener(e -> {
            int row = historyTable.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(this, "Select an order."); return; }
            int mRow   = historyTable.convertRowIndexToModel(row);
            int orderId = (int) historyModel.getValueAt(mRow, 0);
            String cur  = String.valueOf(historyModel.getValueAt(mRow, 3));
            String next = getNextStatus(cur);
            if (next == null) { JOptionPane.showMessageDialog(this, "Order already delivered."); return; }

            int confirm = JOptionPane.showConfirmDialog(this,
                    "Update: " + cur + " → " + next + "?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;

            RestockOrderDB.updateStatus(orderId, cur, next);
            if ("DELIVERED".equals(next)) {
                List<RestockOrderItem> delivered = RestockOrderDB.getOrderItems(orderId);
                for (RestockOrderItem item : delivered)
                    SASync.deductSAStock(item.getItemId(), item.getQuantity());
                refreshMerchantStatus();
                JOptionPane.showMessageDialog(this,
                        "Delivery recorded. Stock updated for " + delivered.size() + " product(s).");
            }
            loadOrderHistory(historyModel);
        });

        JPanel actionBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionBar.setOpaque(false);
        actionBar.add(refreshBtn); actionBar.add(viewItemsBtn); actionBar.add(trackBtn);
        actionBar.add(printBtn);   actionBar.add(invoiceBtn);   actionBar.add(statusBtn);

        JPanel card = new JPanel(new BorderLayout(12, 12));
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(4, 0, 0, 0));
        card.add(scroll,     BorderLayout.CENTER);
        card.add(actionBar,  BorderLayout.SOUTH);
        return card;
    }

    private void switchOrderView(String card) {
        orderCardLayout.show(orderCards, card);
        boolean isNew = CARD_NEW_ORDER.equals(card);
        placeNewOrderTab.setBackground(isNew  ? ThemeManager.buttonDark()  : ThemeManager.buttonLight());
        placeNewOrderTab.setForeground(isNew  ? ThemeManager.textLight()   : ThemeManager.textPrimary());
        placeNewOrderTab.setBorder(isNew
                ? BorderFactory.createEmptyBorder(10, 16, 10, 16)
                : BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(ThemeManager.borderColor()), new EmptyBorder(10, 16, 10, 16)));
        orderHistoryTab.setBackground(!isNew  ? ThemeManager.buttonDark()  : ThemeManager.buttonLight());
        orderHistoryTab.setForeground(!isNew  ? ThemeManager.textLight()   : ThemeManager.textPrimary());
        orderHistoryTab.setBorder(!isNew
                ? BorderFactory.createEmptyBorder(10, 16, 10, 16)
                : BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(ThemeManager.borderColor()), new EmptyBorder(10, 16, 10, 16)));
    }

    private void wireActions() {
        addToOrderBtn.addActionListener(e -> addSelectedProduct());
        removeItemBtn.addActionListener(e -> removeSelectedSummaryItem());
        clearOrderBtn.addActionListener(e -> clearOrder());
        placeOrderBtn.addActionListener(e -> placeOrder());
        placeNewOrderTab.addActionListener(e -> switchOrderView(CARD_NEW_ORDER));
        orderHistoryTab.addActionListener(e -> {
            switchOrderView(CARD_HISTORY);
            loadOrderHistory(historyModel);
        });

        searchField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) {
                if (searchField.getText().equals("Search catalogue...")) {
                    searchField.setText(""); searchField.setForeground(ThemeManager.textPrimary());
                }
            }
            @Override public void focusLost(java.awt.event.FocusEvent e) {
                if (searchField.getText().trim().isEmpty()) {
                    searchField.setText("Search catalogue..."); searchField.setForeground(Color.GRAY);
                }
            }
        });
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { filterCatalogue(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { filterCatalogue(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filterCatalogue(); }
        });

        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentShown(java.awt.event.ComponentEvent e) {
                loadCatalogue(); refreshMerchantStatus();
            }
        });
    }

    private void loadCatalogue() {
        currentCatalogue = MerchantDB.getCatalogueItems();
        populateCatalogueTable(currentCatalogue);
    }

    private void filterCatalogue() {
        String kw = searchField.getText().trim().toLowerCase();
        if (kw.equals("search catalogue...") || kw.isEmpty()) {
            populateCatalogueTable(currentCatalogue); return;
        }
        List<SACatalogueItem> filtered = new ArrayList<>();
        for (SACatalogueItem item : currentCatalogue) {
            if (item.getDescription().toLowerCase().contains(kw)
                    || item.getItemId().toLowerCase().contains(kw))
                filtered.add(item);
        }
        populateCatalogueTable(filtered);
    }

    private void populateCatalogueTable(List<SACatalogueItem> items) {
        catalogueModel.setRowCount(0);
        for (SACatalogueItem item : items) {
            catalogueModel.addRow(new Object[]{
                    item.getItemId(), item.getDescription(),
                    String.format("%.2f", item.getUnitCost()),
                    item.getAvailability()
            });
        }
    }

    private void addSelectedProduct() {
        int row = catalogueTable.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this, "Please select a product from the SA catalogue."); return; }
        String itemId = (String) catalogueModel.getValueAt(row, 0);
        SACatalogueItem item = currentCatalogue.stream()
                .filter(i -> i.getItemId().equals(itemId)).findFirst().orElse(null);
        if (item == null) return;

        String input = JOptionPane.showInputDialog(this, "Quantity to order:");
        if (input == null || input.trim().isEmpty()) return;
        try {
            int qty = Integer.parseInt(input.trim());
            if (qty <= 0) throw new NumberFormatException();
            cart.merge(item, qty, Integer::sum);
            refreshSummary();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid positive quantity.");
        }
    }

    private void removeSelectedSummaryItem() {
        int row = summaryTable.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this, "Please select an item to remove."); return; }
        SACatalogueItem key = (SACatalogueItem) cart.keySet().toArray()[summaryTable.convertRowIndexToModel(row)];
        cart.remove(key);
        refreshSummary();
    }

    private void refreshSummary() {
        summaryModel.setRowCount(0);
        double total = 0;
        for (Map.Entry<SACatalogueItem, Integer> entry : cart.entrySet()) {
            SACatalogueItem item = entry.getKey();
            int qty = entry.getValue();
            double lineTotal = item.getUnitCost() * qty;
            total += lineTotal;
            summaryModel.addRow(new Object[]{
                    item.getItemId(), item.getDescription(), qty,
                    String.format("%.2f", item.getUnitCost()),
                    String.format("%.2f", lineTotal)
            });
        }
        totalLabel.setText("Total: £" + String.format("%.2f", total));
    }

    private void clearOrder() { cart.clear(); refreshSummary(); }

    private void placeOrder() {
        if (!Session.isManagerOrAdmin()) {
            JOptionPane.showMessageDialog(this, "Only Managers and Admins can place orders.");
            return;
        }
        if (cart.isEmpty()) { JOptionPane.showMessageDialog(this, "Order is empty."); return; }

        // GATE: supplier account must be NORMAL
        Merchant merchant = Session.getMerchant();
        if (merchant == null || !merchant.isAccountNormal()) {
            String statusMsg = merchant != null ? merchant.getSaAccountStatus() : "Not configured";
            JOptionPane.showMessageDialog(this,
                    "Cannot place order.\n\nSupplier account status: " + statusMsg
                            + "\n\nOrders can only be placed when the supplier account is NORMAL.\n"
                            + "Please settle your outstanding balance first.",
                    "Supplier Account Restricted", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String merchantId = Session.getMerchantId();

        // Snapshot carts on the EDT before launching background worker
        Map<Product, Integer> productCart = new LinkedHashMap<>();
        Map<String, Integer> saItemsForApi = new LinkedHashMap<>();
        for (Map.Entry<SACatalogueItem, Integer> entry : cart.entrySet()) {
            SACatalogueItem sa = entry.getKey();
            int qty = entry.getValue();
            int productId = MerchantDB.ensureProduct(sa);
            Product p = new Product(productId, sa.getItemId(), sa.getDescription(),
                    "Pack of " + sa.getPackSize(), sa.getPackSize(),
                    sa.getUnitCost(), 0.0, 0, 0);
            productCart.put(p, qty);
            saItemsForApi.put(sa.getItemId(), qty);
        }

        placeOrderBtn.setEnabled(false);
        placeOrderBtn.setText("Placing...");

        new javax.swing.SwingWorker<String[], Void>() {
            @Override
            protected String[] doInBackground() {
                String orderNumber = RestockOrderDB.placeOrder(merchantId, productCart);
                if (orderNumber == null) return null;

                JSONArray itemsJson = new JSONArray();
                for (Map.Entry<String, Integer> e : saItemsForApi.entrySet()) {
                    itemsJson.put(new JSONObject()
                            .put("itemId", e.getKey())
                            .put("quantity", e.getValue()));
                }
                String saOrderId = SASync.placeOrderViaSA(merchantId,
                        new JSONObject().put("items", itemsJson).toString());
                if (saOrderId != null) {
                    RestockOrderDB.updateSAOrderId(orderNumber, saOrderId);
                }
                return new String[]{orderNumber, saOrderId};
            }

            @Override
            protected void done() {
                placeOrderBtn.setEnabled(true);
                placeOrderBtn.setText("Place Order");
                try {
                    String[] result = get();
                    if (result == null) {
                        JOptionPane.showMessageDialog(OrdersPanel.this,
                                "Failed to place order locally. Check logs for details.",
                                "Order Failed", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    String orderNumber = result[0];
                    String saOrderId   = result[1];

                    refreshMerchantStatus();
                    clearOrder();

                    String confirmMsg = "Order placed successfully.\n\nLocal Order:  " + orderNumber
                            + (saOrderId != null
                                    ? "\nSA Order ID: " + saOrderId
                                    : "\n(SA offline — order queued locally)")
                            + "\n\nPrint order form?";
                    int print = JOptionPane.showConfirmDialog(OrdersPanel.this, confirmMsg,
                            "Order Placed", JOptionPane.YES_NO_OPTION);
                    if (print == JOptionPane.YES_OPTION) {
                        RestockOrder placed = RestockOrderDB.getByOrderNumber(orderNumber);
                        if (placed != null) {
                            List<RestockOrderItem> items = RestockOrderDB.getOrderItems(placed.getRestockOrderId());
                            PdfGenerator.generateOrderForm(OrdersPanel.this, placed, items);
                        }
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(OrdersPanel.this,
                            "Order error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void loadOrderHistory(DefaultTableModel model) {
        model.setRowCount(0);
        for (RestockOrder o : RestockOrderDB.getAllOrders()) {
            model.addRow(new Object[]{
                    o.getRestockOrderId(), o.getOrderNumber(), o.getMerchantId(),
                    o.getStatus(), String.format("%.2f", o.getTotalValue()),
                    RestockOrderDB.getSAOrderId(o.getRestockOrderId()),
                    o.getCreatedAt()
            });
        }
    }

    private void showOrderItemsDialog(java.awt.Window parent, int orderId) {
        List<RestockOrderItem> items = RestockOrderDB.getOrderItems(orderId);
        DefaultTableModel m = new DefaultTableModel(
                new String[]{"Item ID", "Description", "Qty", "Unit Cost £", "Line Total £"}, 0
        ) { @Override public boolean isCellEditable(int r, int c) { return false; } };
        double total = 0;
        for (RestockOrderItem item : items) {
            m.addRow(new Object[]{ item.getItemId(), item.getDescription(), item.getQuantity(),
                    String.format("%.2f", item.getUnitCost()), String.format("%.2f", item.getLineTotal()) });
            total += item.getLineTotal();
        }
        JTable t = new JTable(m); configureTable(t); applyTableTheme(t);
        JScrollPane sp = new JScrollPane(t); styleScrollPane(sp);
        sp.setPreferredSize(new Dimension(620, 260));
        JLabel lbl = new JLabel("Grand Total: £" + String.format("%.2f", total));
        lbl.setFont(new Font("SansSerif", Font.BOLD, 13));
        lbl.setBorder(new EmptyBorder(8, 0, 0, 0));
        JPanel c = new JPanel(new BorderLayout(8, 8));
        c.setBorder(new EmptyBorder(12, 12, 12, 12));
        c.setBackground(ThemeManager.panelBackground());
        c.add(sp, BorderLayout.CENTER); c.add(lbl, BorderLayout.SOUTH);
        JDialog d = new JDialog(parent, "Order Items", Dialog.ModalityType.APPLICATION_MODAL);
        d.setContentPane(c); d.pack(); d.setLocationRelativeTo(parent); d.setVisible(true);
    }

    private JPanel buildMerchantStatusCard() {
        JPanel card = AppShell.createCard();
        card.setLayout(new FlowLayout(FlowLayout.LEFT, 14, 6));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));

        merchantStatusLabel = new JLabel("InfoPharma SA:");
        merchantStatusLabel.setFont(new Font("SansSerif", Font.BOLD, 13));

        accountStatusLabel = new JLabel("NORMAL");
        accountStatusLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        accountStatusLabel.setForeground(new Color(34, 139, 34));

        JLabel activeLabel = new JLabel("  Active Orders:");
        activeOrdersValue  = new JLabel("—");
        JLabel outLabel    = new JLabel("  Balance:");
        outstandingValue   = new JLabel("—");

        JButton detailsBtn = createBtn("Account Details", false);
        JButton settleBtn  = createBtn("Record SA Payment", false);
        detailsBtn.addActionListener(e -> showAccountBalanceDialog());
        settleBtn.addActionListener(e  -> showSettleAccountDialog());

        card.add(merchantStatusLabel); card.add(accountStatusLabel);
        card.add(activeLabel); card.add(activeOrdersValue);
        card.add(outLabel); card.add(outstandingValue);
        card.add(detailsBtn); card.add(settleBtn);

        refreshMerchantStatus();
        return card;
    }

    private void refreshMerchantStatus() {
        if (activeOrdersValue == null) return;
        activeOrdersValue.setText(String.valueOf(RestockOrderDB.getActiveOrderCount()));
        Merchant m = Session.getMerchant();
        if (m != null) {
            outstandingValue.setText(String.format("£%.2f", m.getSaBalance()));
            if (accountStatusLabel != null) {
                String status = m.getSaAccountStatus();
                accountStatusLabel.setText(status);
                switch (status) {
                    case "NORMAL"     -> accountStatusLabel.setForeground(new Color(34, 139, 34));
                    case "SUSPENDED"  -> accountStatusLabel.setForeground(new Color(210, 140, 0));
                    case "IN_DEFAULT" -> accountStatusLabel.setForeground(new Color(180, 50, 50));
                    default           -> accountStatusLabel.setForeground(ThemeManager.textPrimary());
                }
            }
        } else {
            outstandingValue.setText("N/A");
            if (accountStatusLabel != null) {
                accountStatusLabel.setText("Not configured");
                accountStatusLabel.setForeground(ThemeManager.textPrimary());
            }
        }
    }

    private void showAccountBalanceDialog() {
        Merchant m = Session.getMerchant();
        if (m == null) { JOptionPane.showMessageDialog(this, "No merchant configured."); return; }
        String msg = String.format(
                "Company          : %s\nMerchant ID      : %s\nReg. Number      : %s\n" +
                "Email            : %s\nPhone            : %s\nAddress          : %s\n" +
                "─────────────────────────────────────────\n" +
                "Account Status   : %s\nDiscount Rate    : %.2f%%\n" +
                "Credit Limit     : £%.2f\nOutstanding      : £%.2f\nAvailable Credit : £%.2f",
                m.getCompanyName(), m.getMerchantId(), m.getRegistrationNumber(),
                m.getEmail(), m.getPhone(), m.getAddress(),
                m.getSaAccountStatus(), m.getSaDiscountRate(),
                m.getSaCreditLimit(), m.getSaBalance(), m.getAvailableCredit());
        JOptionPane.showMessageDialog(this, msg, "InfoPharma SA — Account Details", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showSettleAccountDialog() {
        Merchant m = Session.getMerchant();
        if (m == null) { JOptionPane.showMessageDialog(this, "No merchant configured."); return; }
        double balance = m.getSaBalance();
        if (balance == 0) { JOptionPane.showMessageDialog(this, "No outstanding balance."); return; }
        JOptionPane.showMessageDialog(this,
                String.format("Current SA Balance: £%.2f\n\nPayments must be made directly with InfoPharma SA.\nThe balance shown here will update on the next SA sync.", balance),
                "SA Account Balance", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showOrderInvoiceDialog(java.awt.Window parent, String orderNumber,
                                        String merchantId, String status, String total, String date) {
        String invoice = String.format(
                "ORDER INVOICE — InfoPharma SA\n═══════════════════════════════════\n" +
                        "Order Number : %s\nMerchant ID  : %s\nDate         : %s\n" +
                        "Status       : %s\n───────────────────────────────────\n" +
                        "Total Value  : £%s\n═══════════════════════════════════\n\n" +
                        "Payment due by end of calendar month.\nSupplier: InfoPharma SA, 3 High Level Drive, SE26 3ET",
                orderNumber, merchantId, date, status, total);
        JTextArea area = new JTextArea(invoice);
        area.setEditable(false);
        area.setFont(new Font("Monospaced", Font.PLAIN, 13));
        area.setMargin(new Insets(12, 12, 12, 12));
        JOptionPane.showMessageDialog(parent, new JScrollPane(area),
                "Invoice — " + orderNumber, JOptionPane.PLAIN_MESSAGE);
    }

    private String getNextStatus(String cur) {
        return switch (cur) {
            case "ACCEPTED"  -> "PROCESSED";
            case "PROCESSED" -> "DISPATCHED";
            case "DISPATCHED"-> "DELIVERED";
            default          -> null;
        };
    }

    private void configureTable(JTable t) {
        t.setRowHeight(44); t.setShowGrid(true); t.setIntercellSpacing(new Dimension(1,1));
        t.setFillsViewportHeight(true); t.setBorder(BorderFactory.createEmptyBorder());
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); t.setDefaultEditor(Object.class, null);
        JTableHeader h = t.getTableHeader(); h.setReorderingAllowed(false);
        h.setFont(new Font("SansSerif", Font.BOLD, 13)); h.setBorder(BorderFactory.createEmptyBorder());
        DefaultTableCellRenderer r = new DefaultTableCellRenderer();
        r.setHorizontalAlignment(SwingConstants.LEFT); r.setBorder(new EmptyBorder(0,10,0,10));
        t.setDefaultRenderer(Object.class, r);
    }

    private void styleScrollPane(JScrollPane sp) {
        sp.setBorder(BorderFactory.createEmptyBorder()); sp.setOpaque(true);
        sp.setBackground(ThemeManager.tableBackground());
        sp.getViewport().setBackground(ThemeManager.tableBackground());
        sp.getViewport().setBorder(null);
    }

    private void applyTableTheme(JTable t) {
        t.setBackground(ThemeManager.tableBackground()); t.setForeground(ThemeManager.textPrimary());
        t.setGridColor(ThemeManager.tableGrid()); t.setSelectionBackground(ThemeManager.selectionBackground());
        t.setSelectionForeground(ThemeManager.textPrimary());
        JTableHeader h = t.getTableHeader();
        if (h != null) { h.setBackground(ThemeManager.tableHeaderBackground());
            h.setForeground(ThemeManager.textPrimary()); h.setBorder(BorderFactory.createEmptyBorder()); h.setOpaque(true); }
        DefaultTableCellRenderer r = new DefaultTableCellRenderer();
        r.setBackground(ThemeManager.tableBackground()); r.setForeground(ThemeManager.textPrimary());
        r.setHorizontalAlignment(SwingConstants.LEFT); r.setBorder(new EmptyBorder(0,10,0,10));
        t.setDefaultRenderer(Object.class, r);
    }

    private JButton createBtn(String text, boolean primary) {
        JButton btn = new JButton(text);
        btn.setFocusPainted(false); btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        if (primary) { btn.setBackground(ThemeManager.buttonDark()); btn.setForeground(ThemeManager.textLight()); }
        else { btn.setBackground(ThemeManager.buttonLight()); btn.setForeground(ThemeManager.textPrimary());
            btn.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(ThemeManager.borderColor()), new EmptyBorder(10,16,10,16))); }
        return btn;
    }

    /** Parses the "Status: xxx" line from SA's tracking text. */
    private String parseSAStatus(String trackingText) {
        if (trackingText == null) return null;
        for (String line : trackingText.split("\n")) {
            String t = line.trim();
            if (t.startsWith("Status:")) return t.substring("Status:".length()).trim();
        }
        return null;
    }

    /** Maps SA status strings (lowercase) to CA status strings (uppercase). */
    private String mapSAStatusToCA(String saStatus) {
        if (saStatus == null) return null;
        return switch (saStatus.toLowerCase()) {
            case "pending"    -> "ACCEPTED";
            case "accepted"   -> "PROCESSED";
            case "dispatched" -> "DISPATCHED";
            case "delivered"  -> "DELIVERED";
            default           -> null;
        };
    }

    /** Returns true if {@code next} is a later stage in the order lifecycle than {@code current}. */
    private boolean isStatusProgression(String current, String next) {
        if (next == null) return false;
        java.util.List<String> stages = java.util.List.of("ACCEPTED", "PROCESSED", "DISPATCHED", "DELIVERED");
        return stages.indexOf(next) > stages.indexOf(current);
    }

    @Override
    public void applyTheme() {
        setBackground(ThemeManager.appBackground());
        if (contentPanel  != null) contentPanel.setBackground(ThemeManager.appBackground());
        if (topTabBar     != null) topTabBar.setBackground(ThemeManager.appBackground());
        if (controlsPanel != null) controlsPanel.setBackground(ThemeManager.appBackground());
        if (bottomBar     != null) bottomBar.setBackground(ThemeManager.appBackground());
        if (leftCard      != null) leftCard.setBackground(ThemeManager.panelBackground());
        if (rightCard     != null) rightCard.setBackground(ThemeManager.panelBackground());
        if (availableProductsLabel != null) availableProductsLabel.setForeground(ThemeManager.textPrimary());
        if (orderSummaryLabel      != null) orderSummaryLabel.setForeground(ThemeManager.textPrimary());
        if (merchantIdLabel        != null) merchantIdLabel.setForeground(ThemeManager.textPrimary());
        if (totalLabel             != null) totalLabel.setForeground(ThemeManager.textPrimary());
        if (catalogueTable  != null) applyTableTheme(catalogueTable);
        if (summaryTable    != null) applyTableTheme(summaryTable);
        if (catalogueScrollPane != null) styleScrollPane(catalogueScrollPane);
        if (summaryScrollPane   != null) styleScrollPane(summaryScrollPane);
        repaint(); revalidate();
    }
}