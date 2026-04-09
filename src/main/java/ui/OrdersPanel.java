package ui;

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

    private JTable productsTable;
    private JTable summaryTable;

    private JScrollPane productsScrollPane;
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

    private DefaultTableModel productsModel;
    private DefaultTableModel summaryModel;

    private final Map<Product, Integer> cart = new LinkedHashMap<>();

    public OrdersPanel(ScreenRouter router) {
        this.router = router;

        setLayout(new BorderLayout());
        ThemeManager.register(this);

        AppShell shell = new AppShell(
                router,
                MainFrame.SCREEN_ORDERS,
                "Order Management",
                "Place and track restock orders",
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

        placeNewOrderTab = createRestockStyleButton("Place New Order", false);
        orderHistoryTab = createRestockStyleButton("Order History", false);

        topTabBar.add(placeNewOrderTab);
        topTabBar.add(orderHistoryTab);

        controlsPanel = new JPanel(new BorderLayout(20, 0));
        controlsPanel.setOpaque(false);

        JPanel leftButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        leftButtons.setOpaque(false);

        addToOrderBtn = createRestockStyleButton("+  Add To Order", true);
        removeItemBtn = createRestockStyleButton("Remove Item", false);

        leftButtons.add(addToOrderBtn);
        leftButtons.add(removeItemBtn);

        JPanel rightMerchant = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        rightMerchant.setOpaque(false);

        merchantIdLabel = new JLabel("Merchant ID:");
        merchantIdValue = new JLabel("MERCHANT-001");
        merchantIdValue.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.borderColor()),
                new EmptyBorder(10, 14, 10, 14)
        ));

        rightMerchant.add(merchantIdLabel);
        rightMerchant.add(merchantIdValue);

        controlsPanel.add(leftButtons, BorderLayout.WEST);
        controlsPanel.add(rightMerchant, BorderLayout.EAST);

        JPanel centerSection = new JPanel(new GridLayout(1, 2, 20, 0));
        centerSection.setOpaque(false);

        leftCard = AppShell.createCard();
        leftCard.setLayout(new BorderLayout(12, 12));
        availableProductsLabel = new JLabel("Available Products");
        availableProductsLabel.setFont(new Font("SansSerif", Font.BOLD, 16));

        rightCard = AppShell.createCard();
        rightCard.setLayout(new BorderLayout(12, 12));
        orderSummaryLabel = new JLabel("Order Summary");
        orderSummaryLabel.setFont(new Font("SansSerif", Font.BOLD, 16));

        productsModel = new DefaultTableModel(
                new String[]{"ID", "Item ID", "Description", "Price £", "Stock", "Min Stock"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        productsTable = new JTable(productsModel);

        summaryModel = new DefaultTableModel(
                new String[]{"Description", "Qty", "Unit Cost £", "Line Total £"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        summaryTable = new JTable(summaryModel);

        configureTable(productsTable);
        configureTable(summaryTable);

        productsScrollPane = new JScrollPane(productsTable);
        summaryScrollPane = new JScrollPane(summaryTable);

        styleScrollPane(productsScrollPane);
        styleScrollPane(summaryScrollPane);

        leftCard.add(availableProductsLabel, BorderLayout.NORTH);
        leftCard.add(productsScrollPane, BorderLayout.CENTER);

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

        clearOrderBtn = createRestockStyleButton("Clear Order", false);
        placeOrderBtn = createRestockStyleButton("Place Order", true);

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
                JOptionPane.showMessageDialog(this, "You are already on Place New Order.")
        );

        orderHistoryTab.addActionListener(e -> showOrderHistoryDialog());

        placeNewOrderTab.addActionListener(e ->
                JOptionPane.showMessageDialog(this, "You are already on Place New Order."));

        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                loadCatalogue();
                refreshMerchantStatus();
            }
        });
    }

    private void loadCatalogue() {
        productsModel.setRowCount(0);
        List<Product> products = ProductDB.getAllProducts();

        for (Product p : products) {
            productsModel.addRow(new Object[]{
                    p.getProductId(),
                    p.getItemId(),
                    p.getDescription(),
                    String.format("%.2f", p.getPrice()),
                    p.getStockQuantity(),
                    p.getMinStockLevel()
            });
        }
    }

    private void addSelectedProduct() {
        int row = productsTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a product.");
            return;
        }

        int modelRow = productsTable.convertRowIndexToModel(row);
        int productId = (int) productsModel.getValueAt(modelRow, 0);
        Product product = ProductDB.getById(productId);

        if (product == null) {
            JOptionPane.showMessageDialog(this, "Could not load the selected product.");
            return;
        }

        String input = JOptionPane.showInputDialog(this, "Quantity to order:");
        if (input == null || input.trim().isEmpty()) {
            return;
        }

        try {
            int qty = Integer.parseInt(input.trim());
            if (qty <= 0) {
                throw new NumberFormatException();
            }

            cart.merge(product, qty, Integer::sum);
            refreshSummary();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid positive quantity.");
        }
    }

    private void removeSelectedSummaryItem() {
        int row = summaryTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select an item to remove.");
            return;
        }

        Product key = (Product) cart.keySet().toArray()[summaryTable.convertRowIndexToModel(row)];
        cart.remove(key);
        refreshSummary();
    }

    private void refreshSummary() {
        summaryModel.setRowCount(0);
        double total = 0.0;

        for (Map.Entry<Product, Integer> entry : cart.entrySet()) {
            Product p = entry.getKey();
            int qty = entry.getValue();
            double lineTotal = p.getPrice() * qty;
            total += lineTotal;

            summaryModel.addRow(new Object[]{
                    p.getDescription(),
                    qty,
                    String.format("%.2f", p.getPrice()),
                    String.format("%.2f", lineTotal)
            });
        }

        totalLabel.setText("Total: £" + String.format("%.2f", total));
    }

    private void clearOrder() {
        cart.clear();
        refreshSummary();
    }

    private void placeOrder() {
        if (cart.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Order is empty.");
            return;
        }

        String merchantId = JOptionPane.showInputDialog(this, "Merchant ID:", merchantIdValue.getText());
        if (merchantId == null || merchantId.trim().isEmpty()) {
            return;
        }

        merchantId = merchantId.trim();
        merchantIdValue.setText(merchantId);

        double outstanding = RestockOrderDB.getOutstandingValue();
        double limit = 10000.00; // configurable threshold
        if (outstanding > limit) {
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
                    "Order placed successfully.\nOrder number: " + orderNumber
                    + "\n\nWould you like to print the order form?",
                    "Order Placed", JOptionPane.YES_NO_OPTION);

            if (print == JOptionPane.YES_OPTION) {
                RestockOrder placed = RestockOrderDB.getByOrderNumber(orderNumber);
                if (placed != null) {
                    java.util.List<RestockOrderItem> items =
                            RestockOrderDB.getOrderItems(placed.getRestockOrderId());
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
        ) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        JTable historyTable = new JTable(historyModel);
        configureTable(historyTable);
        applyTableTheme(historyTable);

        JScrollPane historyScroll = new JScrollPane(historyTable);
        styleScrollPane(historyScroll);

        loadOrderHistory(historyModel);

        JButton refreshHistoryBtn  = createRestockStyleButton("Refresh", false);
        JButton viewItemsBtn       = createRestockStyleButton("View Items", false);
        JButton printOrderFormBtn  = createRestockStyleButton("Print Order Form", false);
        JButton updateStatusBtn    = createRestockStyleButton("Update Status", true);
        JButton viewInvoiceBtn = createRestockStyleButton("View Invoice", false);


        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));
        panel.setBackground(ThemeManager.panelBackground());
        panel.add(historyScroll, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        buttons.setOpaque(false);
        buttons.add(refreshHistoryBtn);
        buttons.add(viewItemsBtn);
        buttons.add(printOrderFormBtn);
        buttons.add(updateStatusBtn);
        panel.add(buttons, BorderLayout.SOUTH);
        buttons.add(viewInvoiceBtn);


        JDialog dialog = new JDialog(
                SwingUtilities.getWindowAncestor(this),
                "Order History",
                Dialog.ModalityType.APPLICATION_MODAL
        );
        dialog.setContentPane(panel);
        dialog.setSize(850, 420);
        dialog.setLocationRelativeTo(this);

        refreshHistoryBtn.addActionListener(e -> loadOrderHistory(historyModel));

        viewItemsBtn.addActionListener(e -> {
            int row = historyTable.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(dialog, "Please select an order."); return; }
            int orderId = (int) historyModel.getValueAt(historyTable.convertRowIndexToModel(row), 0);
            showOrderItemsDialog(dialog, orderId);
        });

        printOrderFormBtn.addActionListener(e -> {
            int row = historyTable.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(dialog, "Please select an order."); return; }
            int modelRow = historyTable.convertRowIndexToModel(row);
            int orderId  = (int) historyModel.getValueAt(modelRow, 0);
            String orderNum = String.valueOf(historyModel.getValueAt(modelRow, 1));
            RestockOrder order = RestockOrderDB.getByOrderNumber(orderNum);
            if (order == null) return;
            java.util.List<RestockOrderItem> items = RestockOrderDB.getOrderItems(orderId);
            PdfGenerator.generateOrderForm(dialog, order, items);
        });

        viewInvoiceBtn.addActionListener(e -> {
            int row = historyTable.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(dialog, "Please select an order."); return; }
            int modelRow = historyTable.convertRowIndexToModel(row);
            String orderNum = String.valueOf(historyModel.getValueAt(modelRow, 1));
            String merchantId = String.valueOf(historyModel.getValueAt(modelRow, 2));
            String status = String.valueOf(historyModel.getValueAt(modelRow, 3));
            String total = String.valueOf(historyModel.getValueAt(modelRow, 4));
            String date = String.valueOf(historyModel.getValueAt(modelRow, 5));
            showOrderInvoiceDialog(dialog, orderNum, merchantId, status, total, date);
        });

        updateStatusBtn.addActionListener(e -> {
            int row = historyTable.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(dialog, "Please select an order.");
                return;
            }

            int modelRow = historyTable.convertRowIndexToModel(row);
            int orderId = (int) historyModel.getValueAt(modelRow, 0);
            String currentStatus = String.valueOf(historyModel.getValueAt(modelRow, 3));
            String nextStatus = getNextStatus(currentStatus);

            if (nextStatus == null) {
                JOptionPane.showMessageDialog(dialog, "This order is already delivered.");
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(
                    dialog,
                    "Update status from " + currentStatus + " to " + nextStatus + "?",
                    "Confirm status change",
                    JOptionPane.YES_NO_OPTION
            );

            if (confirm == JOptionPane.YES_OPTION) {
                RestockOrderDB.updateStatus(orderId, currentStatus, nextStatus);

                if ("DELIVERED".equals(nextStatus)) {
                    java.util.List<domain.RestockOrderItem> deliveredItems =
                            RestockOrderDB.getOrderItems(orderId);
                    for (domain.RestockOrderItem item : deliveredItems) {
                        domain.Product p = ProductDB.getByItemId(item.getItemId());
                        if (p != null) {
                            ProductDB.updateStock(p.getProductId(), item.getQuantity());
                        }
                    }
                    JOptionPane.showMessageDialog(dialog,
                            "Delivery recorded. Stock updated for " +
                                    deliveredItems.size() + " product(s).");
                }
                loadOrderHistory(historyModel);
                loadCatalogue();
            }
        });

        dialog.setVisible(true);
    }

    private void loadOrderHistory(DefaultTableModel historyModel) {
        historyModel.setRowCount(0);

        for (RestockOrder order : RestockOrderDB.getAllOrders()) {
            historyModel.addRow(new Object[]{
                    order.getRestockOrderId(),
                    order.getOrderNumber(),
                    order.getMerchantId(),
                    order.getStatus(),
                    String.format("%.2f", order.getTotalValue()),
                    order.getCreatedAt()
            });
        }
    }

    private void showOrderItemsDialog(java.awt.Window parent, int restockOrderId) {
        java.util.List<RestockOrderItem> items = RestockOrderDB.getOrderItems(restockOrderId);

        DefaultTableModel itemModel = new DefaultTableModel(
                new String[]{"Item ID", "Description", "Qty", "Unit Cost £", "Line Total £"}, 0
        ) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        double grandTotal = 0;
        for (RestockOrderItem item : items) {
            itemModel.addRow(new Object[]{
                    item.getItemId(),
                    item.getDescription(),
                    item.getQuantity(),
                    String.format("%.2f", item.getUnitCost()),
                    String.format("%.2f", item.getLineTotal())
            });
            grandTotal += item.getLineTotal();
        }

        JTable itemTable = new JTable(itemModel);
        configureTable(itemTable);
        applyTableTheme(itemTable);

        JScrollPane scroll = new JScrollPane(itemTable);
        styleScrollPane(scroll);
        scroll.setPreferredSize(new Dimension(600, 250));

        JLabel totalLbl = new JLabel("Grand Total: £" + String.format("%.2f", grandTotal));
        totalLbl.setFont(new Font("SansSerif", Font.BOLD, 13));
        totalLbl.setBorder(new EmptyBorder(8, 0, 0, 0));

        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(new EmptyBorder(12, 12, 12, 12));
        content.setBackground(ThemeManager.panelBackground());
        content.add(scroll, BorderLayout.CENTER);
        content.add(totalLbl, BorderLayout.SOUTH);

        JDialog d = new JDialog(parent, "Order Items", Dialog.ModalityType.APPLICATION_MODAL);
        d.setContentPane(content);
        d.pack();
        d.setLocationRelativeTo(parent);
        d.setVisible(true);
    }

    private JPanel buildMerchantStatusCard() {
        JPanel card = AppShell.createCard();
        card.setLayout(new FlowLayout(FlowLayout.LEFT, 18, 6));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));

        merchantStatusLabel = new JLabel("Merchant Account:");
        merchantStatusLabel.setFont(new Font("SansSerif", Font.BOLD, 13));

        JLabel activeLabel = new JLabel("Active Orders:");
        activeOrdersValue  = new JLabel("—");

        JLabel outLabel  = new JLabel("Outstanding Value:");
        outstandingValue = new JLabel("—");

        card.add(merchantStatusLabel);
        card.add(Box.createHorizontalStrut(12));
        card.add(activeLabel);
        card.add(activeOrdersValue);
        card.add(Box.createHorizontalStrut(18));
        card.add(outLabel);
        card.add(outstandingValue);

        JButton balanceBtn = createRestockStyleButton("Check Balance", false);
        card.add(Box.createHorizontalStrut(18));
        card.add(balanceBtn);
        balanceBtn.addActionListener(e -> showAccountBalanceDialog());

        refreshMerchantStatus();
        return card;
    }

    private void refreshMerchantStatus() {
        if (activeOrdersValue == null) return;
        int count        = RestockOrderDB.getActiveOrderCount();
        double outstanding = RestockOrderDB.getOutstandingValue();
        activeOrdersValue.setText(String.valueOf(count));
        outstandingValue.setText(String.format("£%.2f", outstanding));
    }

    private String getNextStatus(String current) {
        return switch (current) {
            case "ACCEPTED" -> "PROCESSED";
            case "PROCESSED" -> "DISPATCHED";
            case "DISPATCHED" -> "DELIVERED";
            default -> null;
        };
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

    private JButton createRestockStyleButton(String text, boolean primary) {
        JButton button = new RoundedButton(text);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setFont(new Font("SansSerif", Font.BOLD, 13));
        button.setHorizontalAlignment(SwingConstants.CENTER);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setBorder(BorderFactory.createEmptyBorder(12, 22, 12, 22));

        if (primary) {
            button.setBackground(ThemeManager.buttonDark());
            button.setForeground(ThemeManager.textLight());
            button.putClientProperty("outlineColor", ThemeManager.buttonDark());
        } else {
            button.setBackground(ThemeManager.buttonLight());
            button.setForeground(ThemeManager.textPrimary());
            button.putClientProperty("outlineColor", ThemeManager.borderColor());
        }

        return button;
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
            if (getModel().isPressed()) {
                fill = fill.darker();
            } else if (getModel().isRollover()) {
                fill = adjust(fill, 8);
            }

            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 26, 26);
            g2.dispose();

            super.paintComponent(g);
        }

        @Override
        protected void paintBorder(Graphics g) {
            Object value = getClientProperty("outlineColor");
            Color outline = value instanceof Color ? (Color) value : new Color(210, 210, 210);

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(outline);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 26, 26);
            g2.dispose();
        }

        private Color adjust(Color c, int amount) {
            int r = Math.min(255, c.getRed() + amount);
            int g = Math.min(255, c.getGreen() + amount);
            int b = Math.min(255, c.getBlue() + amount);
            return new Color(r, g, b, c.getAlpha());
        }
    }

    @Override
    public void applyTheme() {
        setBackground(ThemeManager.appBackground());

        if (contentPanel != null) contentPanel.setBackground(ThemeManager.appBackground());
        if (topTabBar != null) topTabBar.setBackground(ThemeManager.appBackground());
        if (controlsPanel != null) controlsPanel.setBackground(ThemeManager.appBackground());
        if (bottomBar != null) bottomBar.setBackground(ThemeManager.appBackground());

        if (leftCard != null) leftCard.setBackground(ThemeManager.panelBackground());
        if (rightCard != null) rightCard.setBackground(ThemeManager.panelBackground());

        if (availableProductsLabel != null) availableProductsLabel.setForeground(ThemeManager.textPrimary());
        if (orderSummaryLabel != null) orderSummaryLabel.setForeground(ThemeManager.textPrimary());
        if (merchantIdLabel != null) merchantIdLabel.setForeground(ThemeManager.textPrimary());

        if (merchantIdValue != null) {
            merchantIdValue.setForeground(ThemeManager.textPrimary());
            merchantIdValue.setBackground(ThemeManager.fieldBackground());
            merchantIdValue.setOpaque(true);
            merchantIdValue.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ThemeManager.borderColor()),
                    new EmptyBorder(10, 14, 10, 14)
            ));
        }

        if (totalLabel != null) totalLabel.setForeground(ThemeManager.textPrimary());

        if (productsTable != null) applyTableTheme(productsTable);
        if (summaryTable != null) applyTableTheme(summaryTable);
        if (productsScrollPane != null) styleScrollPane(productsScrollPane);
        if (summaryScrollPane != null) styleScrollPane(summaryScrollPane);

        restyleButton(addToOrderBtn, true);
        restyleButton(removeItemBtn, false);
        restyleButton(clearOrderBtn, false);
        restyleButton(placeOrderBtn, true);
        restyleButton(placeNewOrderTab, false);
        restyleButton(orderHistoryTab, false);

        repaint();
        revalidate();
    }

    private void restyleButton(JButton button, boolean primary) {
        if (button == null) {
            return;
        }

        if (primary) {
            button.setBackground(ThemeManager.buttonDark());
            button.setForeground(ThemeManager.textLight());
            button.putClientProperty("outlineColor", ThemeManager.buttonDark());
        } else {
            button.setBackground(ThemeManager.buttonLight());
            button.setForeground(ThemeManager.textPrimary());
            button.putClientProperty("outlineColor", ThemeManager.borderColor());
        }
    }

    private void showAccountBalanceDialog() {
        int activeOrders    = RestockOrderDB.getActiveOrderCount();
        double outstanding  = RestockOrderDB.getOutstandingValue();

        String message = String.format(
                "Merchant Account Summary\n\n" +
                        "Active Orders:       %d\n" +
                        "Outstanding Balance: £%.2f\n\n" +
                        "Note: Balance represents the total value of\n" +
                        "accepted/processed/dispatched orders not yet delivered.",
                activeOrders, outstanding
        );

        JOptionPane.showMessageDialog(this, message,
                "Account Balance", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showOrderInvoiceDialog(java.awt.Window parent, String orderNumber,
                                        String merchantId, String status, String total, String date) {

        String invoice = String.format(
                "ORDER INVOICE\n" +
                        "═══════════════════════════════\n" +
                        "Order Number : %s\n" +
                        "Merchant ID  : %s\n" +
                        "Date         : %s\n" +
                        "Status       : %s\n" +
                        "───────────────────────────────\n" +
                        "Total Value  : £%s\n" +
                        "═══════════════════════════════\n\n" +
                        "Payment is due by end of calendar month.",
                orderNumber, merchantId, date, status, total
        );

        JTextArea area = new JTextArea(invoice);
        area.setEditable(false);
        area.setFont(new Font("Monospaced", Font.PLAIN, 13));
        area.setMargin(new Insets(12, 12, 12, 12));

        JOptionPane.showMessageDialog(parent, new JScrollPane(area),
                "Invoice — " + orderNumber, JOptionPane.PLAIN_MESSAGE);
    }
}