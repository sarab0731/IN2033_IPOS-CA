package ui;

import app.Session;
import database.ProductDB;
import database.RestockOrderDB;
import domain.Product;
import domain.RestockOrder;
import domain.RestockOrderItem;
import domain.SACatalogueItem;
import integration.SACatalogueService;

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

    private JPanel contentPanel;
    private JPanel topTabBar;
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
    private JButton placeNewOrderTab;
    private JButton orderHistoryTab;

    private JLabel availableProductsLabel;
    private JLabel orderSummaryLabel;
    private JLabel merchantIdLabel;
    private JLabel merchantIdValue;
    private JLabel totalLabel;

    private JLabel merchantStatusLabel;
    private JLabel activeOrdersValue;
    private JLabel outstandingValue;
    private JLabel accountStatusLabel;

    private JComboBox<String> categoryCombo;
    private JTextField searchField;

    private DefaultTableModel catalogueModel;
    private DefaultTableModel summaryModel;

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

        topTabBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 18, 0));
        topTabBar.setOpaque(false);
        placeNewOrderTab = createBtn("Place New Order", false);
        orderHistoryTab  = createBtn("Order History", false);
        topTabBar.add(placeNewOrderTab);
        topTabBar.add(orderHistoryTab);

        controlsPanel = new JPanel(new BorderLayout(20, 0));
        controlsPanel.setOpaque(false);

        JPanel leftButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftButtons.setOpaque(false);

        addToOrderBtn = createBtn("+  Add To Order", true);
        removeItemBtn = createBtn("Remove Item", false);

        categoryCombo = new JComboBox<>(SACatalogueService.getCategories().toArray(new String[0]));
        categoryCombo.setFont(new Font("SansSerif", Font.PLAIN, 13));
        categoryCombo.setPreferredSize(new Dimension(160, 36));

        searchField = new JTextField("Search catalogue...");
        searchField.setPreferredSize(new Dimension(170, 36));
        searchField.setFont(new Font("SansSerif", Font.PLAIN, 13));
        searchField.setForeground(Color.GRAY);

        leftButtons.add(addToOrderBtn);
        leftButtons.add(removeItemBtn);
        leftButtons.add(new JLabel("  Category:"));
        leftButtons.add(categoryCombo);
        leftButtons.add(searchField);

        JPanel rightMerchant = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        rightMerchant.setOpaque(false);
        merchantIdLabel = new JLabel("Merchant ID:");
        merchantIdValue = new JLabel("MERCHANT-001");
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
                new String[]{"Item ID", "Description", "Category", "Unit Cost £", "Pack Size", "Manufacturer"}, 0
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

        JPanel wrapper = new JPanel(new BorderLayout(20, 20));
        wrapper.setOpaque(false);
        wrapper.add(controlsPanel, BorderLayout.NORTH);
        wrapper.add(centerSection, BorderLayout.CENTER);
        wrapper.add(bottomBar, BorderLayout.SOUTH);

        JPanel merchantCard = buildMerchantStatusCard();

        JPanel northStack = new JPanel();
        northStack.setLayout(new BoxLayout(northStack, BoxLayout.Y_AXIS));
        northStack.setOpaque(false);
        northStack.add(topTabBar);
        northStack.add(Box.createVerticalStrut(8));
        northStack.add(merchantCard);

        contentPanel.add(northStack, BorderLayout.NORTH);
        contentPanel.add(wrapper, BorderLayout.CENTER);
        return contentPanel;
    }

    private void wireActions() {
        addToOrderBtn.addActionListener(e -> addSelectedProduct());
        removeItemBtn.addActionListener(e -> removeSelectedSummaryItem());
        clearOrderBtn.addActionListener(e -> clearOrder());
        placeOrderBtn.addActionListener(e -> placeOrder());
        placeNewOrderTab.addActionListener(e ->
                JOptionPane.showMessageDialog(this, "You are already on Place New Order."));
        orderHistoryTab.addActionListener(e -> showOrderHistoryDialog());
        categoryCombo.addActionListener(e -> loadCatalogue());

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
        String category = (String) categoryCombo.getSelectedItem();
        currentCatalogue = SACatalogueService.getCatalogueByCategory(category);
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
                    || item.getItemId().toLowerCase().contains(kw)
                    || item.getCategory().toLowerCase().contains(kw))
                filtered.add(item);
        }
        populateCatalogueTable(filtered);
    }

    private void populateCatalogueTable(List<SACatalogueItem> items) {
        catalogueModel.setRowCount(0);
        for (SACatalogueItem item : items) {
            catalogueModel.addRow(new Object[]{
                    item.getItemId(), item.getDescription(), item.getCategory(),
                    String.format("%.2f", item.getUnitCost()),
                    item.getPackSize(), item.getManufacturer()
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
        if (!SACatalogueService.isAccountNormal()) {
            JOptionPane.showMessageDialog(this,
                    "Cannot place order.\n\nSupplier account status: "
                            + SACatalogueService.getAccountStatus().name()
                            + "\n\nOrders can only be placed when the supplier account is NORMAL.\n"
                            + "Please settle your outstanding balance first.",
                    "Supplier Account Restricted", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String merchantId = JOptionPane.showInputDialog(this, "Merchant ID:", merchantIdValue.getText());
        if (merchantId == null || merchantId.trim().isEmpty()) return;
        merchantId = merchantId.trim();
        merchantIdValue.setText(merchantId);

        // Convert SA catalogue items to Product map for RestockOrderDB
        Map<Product, Integer> productCart = new LinkedHashMap<>();
        double orderTotal = 0;
        for (Map.Entry<SACatalogueItem, Integer> entry : cart.entrySet()) {
            SACatalogueItem sa = entry.getKey();
            int qty = entry.getValue();
            Product p = new Product(0, sa.getItemId(), sa.getDescription(),
                    "Pack of " + sa.getPackSize(), sa.getPackSize(),
                    sa.getUnitCost(), 0.0, 0, 0);
            productCart.put(p, qty);
            orderTotal += sa.getUnitCost() * qty;
        }

        String orderNumber = RestockOrderDB.placeOrder(merchantId, productCart);
        if (orderNumber != null) {
            SACatalogueService.addToBalance(orderTotal);
            refreshMerchantStatus();
            clearOrder();

            int print = JOptionPane.showConfirmDialog(this,
                    "Order placed with InfoPharma SA.\nOrder: " + orderNumber + "\n\nPrint order form?",
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

    private void showOrderHistoryDialog() {
        DefaultTableModel historyModel = new DefaultTableModel(
                new String[]{"ID", "Order Number", "Merchant ID", "Status", "Total £", "Created At"}, 0
        ) { @Override public boolean isCellEditable(int r, int c) { return false; } };

        JTable historyTable = new JTable(historyModel);
        configureTable(historyTable); applyTableTheme(historyTable);
        JScrollPane historyScroll = new JScrollPane(historyTable);
        styleScrollPane(historyScroll);
        loadOrderHistory(historyModel);

        JButton refreshBtn  = createBtn("Refresh", false);
        JButton viewItemsBtn = createBtn("View Items", false);
        JButton printBtn    = createBtn("Print Order Form", false);
        JButton invoiceBtn  = createBtn("View Invoice", false);
        JButton statusBtn   = createBtn("Update Status", true);

        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));
        panel.setBackground(ThemeManager.panelBackground());
        panel.add(historyScroll, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        buttons.add(refreshBtn); buttons.add(viewItemsBtn);
        buttons.add(printBtn); buttons.add(invoiceBtn); buttons.add(statusBtn);
        panel.add(buttons, BorderLayout.SOUTH);

        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this),
                "Order History — InfoPharma SA", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setContentPane(panel);
        dialog.setSize(920, 440);
        dialog.setLocationRelativeTo(this);

        refreshBtn.addActionListener(e -> loadOrderHistory(historyModel));

        viewItemsBtn.addActionListener(e -> {
            int row = historyTable.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(dialog, "Select an order."); return; }
            showOrderItemsDialog(dialog, (int) historyModel.getValueAt(historyTable.convertRowIndexToModel(row), 0));
        });

        printBtn.addActionListener(e -> {
            int row = historyTable.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(dialog, "Select an order."); return; }
            int mRow = historyTable.convertRowIndexToModel(row);
            String num = String.valueOf(historyModel.getValueAt(mRow, 1));
            RestockOrder order = RestockOrderDB.getByOrderNumber(num);
            if (order == null) return;
            PdfGenerator.generateOrderForm(dialog, order, RestockOrderDB.getOrderItems((int) historyModel.getValueAt(mRow, 0)));
        });

        invoiceBtn.addActionListener(e -> {
            int row = historyTable.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(dialog, "Select an order."); return; }
            int mRow = historyTable.convertRowIndexToModel(row);
            showOrderInvoiceDialog(dialog,
                    String.valueOf(historyModel.getValueAt(mRow, 1)),
                    String.valueOf(historyModel.getValueAt(mRow, 2)),
                    String.valueOf(historyModel.getValueAt(mRow, 3)),
                    String.valueOf(historyModel.getValueAt(mRow, 4)),
                    String.valueOf(historyModel.getValueAt(mRow, 5)));
        });

        statusBtn.addActionListener(e -> {
            int row = historyTable.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(dialog, "Select an order."); return; }
            int mRow = historyTable.convertRowIndexToModel(row);
            int orderId = (int) historyModel.getValueAt(mRow, 0);
            String cur  = String.valueOf(historyModel.getValueAt(mRow, 3));
            String next = getNextStatus(cur);
            if (next == null) { JOptionPane.showMessageDialog(dialog, "Order already delivered."); return; }

            int confirm = JOptionPane.showConfirmDialog(dialog,
                    "Update: " + cur + " → " + next + "?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;

            RestockOrderDB.updateStatus(orderId, cur, next);
            if ("DELIVERED".equals(next)) {
                List<RestockOrderItem> delivered = RestockOrderDB.getOrderItems(orderId);
                for (RestockOrderItem item : delivered) {
                    Product p = ProductDB.getByItemId(item.getItemId());
                    if (p != null) ProductDB.updateStock(p.getProductId(), item.getQuantity());
                }
                double val = delivered.stream().mapToDouble(RestockOrderItem::getLineTotal).sum();
                SACatalogueService.recordPayment(val);
                refreshMerchantStatus();
                JOptionPane.showMessageDialog(dialog, "Delivery recorded. Stock updated for " + delivered.size() + " product(s).");
            }
            loadOrderHistory(historyModel);
        });

        dialog.setVisible(true);
    }

    private void loadOrderHistory(DefaultTableModel model) {
        model.setRowCount(0);
        for (RestockOrder o : RestockOrderDB.getAllOrders()) {
            model.addRow(new Object[]{
                    o.getRestockOrderId(), o.getOrderNumber(), o.getMerchantId(),
                    o.getStatus(), String.format("%.2f", o.getTotalValue()), o.getCreatedAt()
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
        outstandingValue.setText(String.format("£%.2f", SACatalogueService.getAccountBalance()));
        if (accountStatusLabel != null) {
            SACatalogueService.AccountStatus s = SACatalogueService.getAccountStatus();
            accountStatusLabel.setText(s.name());
            switch (s) {
                case NORMAL     -> accountStatusLabel.setForeground(new Color(34, 139, 34));
                case SUSPENDED  -> accountStatusLabel.setForeground(new Color(210, 140, 0));
                case IN_DEFAULT -> accountStatusLabel.setForeground(new Color(180, 50, 50));
            }
        }
    }

    private void showAccountBalanceDialog() {
        JOptionPane.showMessageDialog(this,
                SACatalogueService.getAccountStatusSummary(),
                "InfoPharma SA — Account Details", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showSettleAccountDialog() {
        double balance = SACatalogueService.getAccountBalance();
        if (balance == 0) { JOptionPane.showMessageDialog(this, "No outstanding balance."); return; }
        JTextField amtField = new JTextField(String.format("%.2f", balance));
        int r = JOptionPane.showConfirmDialog(this,
                new Object[]{"Current SA Balance: £" + String.format("%.2f", balance), "Payment Amount £:", amtField},
                "Record Payment to InfoPharma SA", JOptionPane.OK_CANCEL_OPTION);
        if (r != JOptionPane.OK_OPTION) return;
        try {
            double amt = Double.parseDouble(amtField.getText().trim());
            if (amt <= 0) throw new NumberFormatException();
            SACatalogueService.recordPayment(amt);
            refreshMerchantStatus();
            JOptionPane.showMessageDialog(this,
                    "Payment of £" + String.format("%.2f", amt) + " recorded.\n"
                            + "New balance: £" + String.format("%.2f", SACatalogueService.getAccountBalance()) + "\n"
                            + "Status: " + SACatalogueService.getAccountStatus().name());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid amount.");
        }
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