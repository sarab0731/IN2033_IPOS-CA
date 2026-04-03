
package ui;

import database.ProductDB;
import database.RestockOrderDB;
import domain.Product;
import domain.RestockOrder;

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

        topTabBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 18, 0));
        topTabBar.setOpaque(false);

        placeNewOrderTab = new JButton("Place New Order");
        orderHistoryTab = new JButton("Order History");
        placeNewOrderTab.setFocusable(false);
        orderHistoryTab.setFocusable(false);
        topTabBar.add(placeNewOrderTab);
        topTabBar.add(orderHistoryTab);

        controlsPanel = new JPanel(new BorderLayout(20, 0));
        controlsPanel.setOpaque(false);

        JPanel leftButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        leftButtons.setOpaque(false);

        addToOrderBtn = new JButton("+  Add To Order");
        removeItemBtn = new JButton("Remove Item");

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
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        productsTable = new JTable(productsModel);

        summaryModel = new DefaultTableModel(
                new String[]{"Description", "Qty", "Unit Cost £", "Line Total £"}, 0
        ) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
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

        clearOrderBtn = new JButton("Clear Order");
        placeOrderBtn = new JButton("Place Order");

        bottomBar.add(clearOrderBtn);
        bottomBar.add(placeOrderBtn);

        JPanel wrapper = new JPanel(new BorderLayout(20, 20));
        wrapper.setOpaque(false);
        wrapper.add(controlsPanel, BorderLayout.NORTH);
        wrapper.add(centerSection, BorderLayout.CENTER);
        wrapper.add(bottomBar, BorderLayout.SOUTH);

        contentPanel.add(topTabBar, BorderLayout.NORTH);
        contentPanel.add(wrapper, BorderLayout.CENTER);

        return contentPanel;
    }

    private void wireActions() {
        addToOrderBtn.addActionListener(e -> addSelectedProduct());
        removeItemBtn.addActionListener(e -> removeSelectedSummaryItem());
        clearOrderBtn.addActionListener(e -> clearOrder());
        placeOrderBtn.addActionListener(e -> placeOrder());

        placeNewOrderTab.addActionListener(e -> JOptionPane.showMessageDialog(
                this,
                "You are already on Place New Order."
        ));

        orderHistoryTab.addActionListener(e -> showOrderHistoryDialog());

        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                loadCatalogue();
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

        int productId = (int) productsModel.getValueAt(row, 0);
        Product product = ProductDB.getById(productId);
        if (product == null) return;

        String input = JOptionPane.showInputDialog(this, "Quantity to order:");
        if (input == null || input.trim().isEmpty()) return;

        try {
            int qty = Integer.parseInt(input.trim());
            if (qty <= 0) throw new NumberFormatException();

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

        Product key = (Product) cart.keySet().toArray()[row];
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

        merchantIdValue.setText(merchantId.trim());
        String orderNumber = RestockOrderDB.placeOrder(merchantId.trim(), cart);

        if (orderNumber != null) {
            JOptionPane.showMessageDialog(this, "Order placed successfully.\nOrder number: " + orderNumber);
            clearOrder();
        } else {
            JOptionPane.showMessageDialog(this, "Failed to place order.");
        }
    }

    private void showOrderHistoryDialog() {
        DefaultTableModel historyModel = new DefaultTableModel(
                new String[]{"ID", "Order Number", "Merchant ID", "Status", "Total £", "Created At"}, 0
        ) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };

        JTable historyTable = new JTable(historyModel);
        configureTable(historyTable);
        loadOrderHistory(historyModel);

        JButton refreshHistoryBtn = new JButton("Refresh");
        JButton updateStatusBtn = new JButton("Update Status");
        styleSecondaryButton(refreshHistoryBtn);
        stylePrimaryButton(updateStatusBtn);

        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.add(new JScrollPane(historyTable), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        buttons.add(refreshHistoryBtn);
        buttons.add(updateStatusBtn);
        panel.add(buttons, BorderLayout.SOUTH);

        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Order History", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setContentPane(panel);
        dialog.setSize(850, 420);
        dialog.setLocationRelativeTo(this);

        refreshHistoryBtn.addActionListener(e -> loadOrderHistory(historyModel));
        updateStatusBtn.addActionListener(e -> {
            int row = historyTable.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(dialog, "Please select an order.");
                return;
            }

            int orderId = (int) historyModel.getValueAt(row, 0);
            String currentStatus = String.valueOf(historyModel.getValueAt(row, 3));
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

    private void stylePrimaryButton(JButton button) {
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
        button.setFont(new Font("SansSerif", Font.BOLD, 13));
        button.setBackground(ThemeManager.buttonDark());
        button.setForeground(ThemeManager.textLight());
    }

    private void styleSecondaryButton(JButton button) {
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createLineBorder(ThemeManager.borderColor()));
        button.setFont(new Font("SansSerif", Font.BOLD, 13));
        button.setBackground(ThemeManager.buttonLight());
        button.setForeground(ThemeManager.textPrimary());
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

        if (placeNewOrderTab != null) styleSecondaryButton(placeNewOrderTab);
        if (orderHistoryTab != null) styleSecondaryButton(orderHistoryTab);
        if (addToOrderBtn != null) stylePrimaryButton(addToOrderBtn);
        if (placeOrderBtn != null) stylePrimaryButton(placeOrderBtn);
        if (removeItemBtn != null) styleSecondaryButton(removeItemBtn);
        if (clearOrderBtn != null) styleSecondaryButton(clearOrderBtn);

        repaint();
        revalidate();
    }
}
