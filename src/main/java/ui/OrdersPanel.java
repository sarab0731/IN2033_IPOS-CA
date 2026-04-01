package ui;

import database.ProductDB;
import database.RestockOrderDB;
import domain.Product;
import domain.RestockOrder;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OrdersPanel extends JPanel {

    private final Map<Product, Integer> cart = new LinkedHashMap<>();

    private DefaultTableModel catalogueModel;
    private DefaultTableModel cartModel;
    private DefaultTableModel ordersModel;

    private JTable catalogueTable;
    private JTable cartTable;
    private JTable ordersTable;

    private JLabel totalLabel;
    private JTextField merchantField;
    private JComboBox<String> historyFilterCombo;

    private static final String[] STATUS_FLOW = {"ACCEPTED", "PROCESSED", "DISPATCHED", "DELIVERED"};

    public OrdersPanel(ScreenRouter router) {
        setLayout(new BorderLayout());
        setBackground(new Color(242, 242, 242));

        JPanel ordersContent = buildOrdersContent();

        AppShell shell = new AppShell(
                router,
                MainFrame.SCREEN_ORDERS,
                "Order Management",
                "Place and track restock orders",
                ordersContent
        );

        add(shell, BorderLayout.CENTER);

        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                loadCatalogue();
                loadOrderHistory();
                clearCart();
            }
        });
    }

    private JPanel buildOrdersContent() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setOpaque(false);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Place New Order", buildNewOrderTab());
        tabs.addTab("Order History", buildOrderHistoryTab());

        outer.add(tabs, BorderLayout.CENTER);
        return outer;
    }

    private JPanel buildNewOrderTab() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setOpaque(false);

        JPanel card = AppShell.createCard();
        card.setLayout(new BorderLayout(0, 18));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(232, 232, 232)),
                new EmptyBorder(20, 20, 20, 20)
        ));

        card.add(buildOrderTopBar(), BorderLayout.NORTH);
        card.add(buildOrderTablesSection(), BorderLayout.CENTER);
        card.add(buildOrderFooter(), BorderLayout.SOUTH);

        outer.add(card, BorderLayout.CENTER);
        return outer;
    }

    private JPanel buildOrderTopBar() {
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);

        JButton addBtn = buildDarkButton("+  Add To Order");
        JButton removeBtn = buildLightButton("Remove Item");

        addBtn.addActionListener(e -> addSelectedProductToCart());
        removeBtn.addActionListener(e -> removeSelectedCartItem());

        left.add(addBtn);
        left.add(removeBtn);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);

        right.add(new JLabel("Merchant ID:"));

        merchantField = new JTextField("MERCHANT-001", 14);
        merchantField.setPreferredSize(new Dimension(160, 32));
        merchantField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)),
                new EmptyBorder(6, 10, 6, 10)
        ));

        right.add(merchantField);

        top.add(left, BorderLayout.WEST);
        top.add(right, BorderLayout.EAST);

        return top;
    }

    private JPanel buildOrderTablesSection() {
        JPanel center = new JPanel(new GridLayout(1, 2, 18, 18));
        center.setOpaque(false);

        center.add(buildCataloguePanel());
        center.add(buildCartPanel());

        return center;
    }

    private JPanel buildCataloguePanel() {
        JPanel panel = buildInnerCard("Available Products");

        String[] columns = {"ID", "Item ID", "Description", "Price £", "Stock", "Min Stock"};
        catalogueModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        catalogueTable = new JTable(catalogueModel);
        styleTable(catalogueTable);

        JScrollPane scrollPane = new JScrollPane(catalogueTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildCartPanel() {
        JPanel panel = buildInnerCard("Order Summary");

        String[] columns = {"Description", "Qty", "Unit Cost £", "Line Total £"};
        cartModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        cartTable = new JTable(cartModel);
        styleTable(cartTable);

        totalLabel = new JLabel("Total: £0.00", SwingConstants.RIGHT);
        totalLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        totalLabel.setForeground(new Color(35, 35, 35));

        JScrollPane scrollPane = new JScrollPane(cartTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(totalLabel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel buildOrderFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        footer.setOpaque(false);

        JButton clearBtn = buildLightButton("Clear Order");
        JButton placeBtn = buildDarkButton("Place Order");

        clearBtn.addActionListener(e -> clearCart());

        placeBtn.addActionListener(e -> {
            if (cart.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Order is empty.");
                return;
            }

            String merchantId = merchantField.getText().trim();
            if (merchantId.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter a merchant ID.");
                return;
            }

            String orderNumber = RestockOrderDB.placeOrder(merchantId, cart);
            if (orderNumber != null) {
                JOptionPane.showMessageDialog(
                        this,
                        "Order placed successfully.\nOrder number: " + orderNumber
                );
                clearCart();
                loadOrderHistory();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to place order. Please try again.");
            }
        });

        footer.add(clearBtn);
        footer.add(placeBtn);

        return footer;
    }

    private JPanel buildOrderHistoryTab() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setOpaque(false);

        JPanel card = AppShell.createCard();
        card.setLayout(new BorderLayout(0, 18));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(232, 232, 232)),
                new EmptyBorder(20, 20, 20, 20)
        ));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);
        JLabel title = new JLabel("Order History");
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        left.add(title);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);

        historyFilterCombo = new JComboBox<>(new String[]{
                "All Orders", "Accepted", "Processed", "Dispatched", "Delivered"
        });
        styleCombo(historyFilterCombo);
        historyFilterCombo.addActionListener(e -> loadOrderHistory());

        JButton updateBtn = buildLightButton("Update Status");
        JButton refreshBtn = buildLightButton("Refresh");

        updateBtn.addActionListener(e -> updateSelectedOrderStatus());
        refreshBtn.addActionListener(e -> loadOrderHistory());

        right.add(historyFilterCombo);
        right.add(updateBtn);
        right.add(refreshBtn);

        top.add(left, BorderLayout.WEST);
        top.add(right, BorderLayout.EAST);

        String[] cols = {"ID", "Order Number", "Merchant ID", "Status", "Total £", "Created At"};
        ordersModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        ordersTable = new JTable(ordersModel);
        styleTable(ordersTable);
        ordersTable.getColumnModel().getColumn(3).setCellRenderer(new StatusCellRenderer());

        JScrollPane scrollPane = new JScrollPane(ordersTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        card.add(top, BorderLayout.NORTH);
        card.add(scrollPane, BorderLayout.CENTER);

        outer.add(card, BorderLayout.CENTER);
        return outer;
    }

    private void loadCatalogue() {
        if (catalogueModel == null) {
            return;
        }

        catalogueModel.setRowCount(0);
        for (Product p : ProductDB.getAllProducts()) {
            catalogueModel.addRow(new Object[]{
                    p.getProductId(),
                    p.getItemId(),
                    p.getDescription(),
                    String.format("%.2f", p.getPrice()),
                    p.getStockQuantity(),
                    p.getMinStockLevel()
            });
        }
    }

    private void addSelectedProductToCart() {
        int row = catalogueTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a product.");
            return;
        }

        int productId = (int) catalogueModel.getValueAt(row, 0);
        Product product = ProductDB.getAllProducts().stream()
                .filter(p -> p.getProductId() == productId)
                .findFirst()
                .orElse(null);

        if (product == null) {
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
            refreshCart();

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid quantity.");
        }
    }

    private void removeSelectedCartItem() {
        int row = cartTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select an item to remove.");
            return;
        }

        Product key = (Product) cart.keySet().toArray()[row];
        cart.remove(key);
        refreshCart();
    }

    private void refreshCart() {
        if (cartModel == null) {
            return;
        }

        cartModel.setRowCount(0);

        double total = 0;
        for (Map.Entry<Product, Integer> entry : cart.entrySet()) {
            Product p = entry.getKey();
            int qty = entry.getValue();
            double lineTotal = p.getPrice() * qty;
            total += lineTotal;

            cartModel.addRow(new Object[]{
                    p.getDescription(),
                    qty,
                    String.format("%.2f", p.getPrice()),
                    String.format("%.2f", lineTotal)
            });
        }

        totalLabel.setText("Total: £" + String.format("%.2f", total));
    }

    private void clearCart() {
        cart.clear();
        refreshCart();
        if (merchantField != null) {
            merchantField.setText("MERCHANT-001");
        }
    }

    private void loadOrderHistory() {
        if (ordersModel == null) {
            return;
        }

        ordersModel.setRowCount(0);
        List<RestockOrder> orders = RestockOrderDB.getAllOrders();

        String filter = historyFilterCombo != null ? (String) historyFilterCombo.getSelectedItem() : "All Orders";

        for (RestockOrder o : orders) {
            if (!"All Orders".equals(filter) && !o.getStatus().equalsIgnoreCase(filter)) {
                continue;
            }

            ordersModel.addRow(new Object[]{
                    o.getRestockOrderId(),
                    o.getOrderNumber(),
                    o.getMerchantId(),
                    o.getStatus(),
                    String.format("%.2f", o.getTotalValue()),
                    o.getCreatedAt()
            });
        }
    }

    private void updateSelectedOrderStatus() {
        int row = ordersTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select an order.");
            return;
        }

        int orderId = (int) ordersModel.getValueAt(row, 0);
        String curStatus = (String) ordersModel.getValueAt(row, 3);

        String nextStatus = getNextStatus(curStatus);
        if (nextStatus == null) {
            JOptionPane.showMessageDialog(this, "This order is already delivered.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Update status from " + curStatus + " to " + nextStatus + "?",
                "Confirm",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            RestockOrderDB.updateStatus(orderId, curStatus, nextStatus);
            loadOrderHistory();
        }
    }

    private String getNextStatus(String current) {
        for (int i = 0; i < STATUS_FLOW.length; i++) {
            if (STATUS_FLOW[i].equalsIgnoreCase(current)) {
                if (i == STATUS_FLOW.length - 1) {
                    return null;
                }
                return STATUS_FLOW[i + 1];
            }
        }
        return null;
    }

    private JPanel buildInnerCard(String titleText) {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(235, 235, 235)),
                new EmptyBorder(12, 12, 12, 12)
        ));
        panel.setBackground(Color.WHITE);

        JLabel title = new JLabel(titleText);
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        title.setForeground(new Color(35, 35, 35));

        panel.add(title, BorderLayout.NORTH);
        return panel;
    }

    private void styleTable(JTable table) {
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(38);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFont(new Font("SansSerif", Font.PLAIN, 12));
        table.setForeground(new Color(35, 35, 35));
        table.setSelectionBackground(new Color(235, 235, 235));
        table.setSelectionForeground(new Color(35, 35, 35));
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.getTableHeader().setReorderingAllowed(false);
    }

    private JButton buildDarkButton(String text) {
        JButton btn = new JButton(text);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(true);
        btn.setOpaque(true);
        btn.setBackground(new Color(30, 32, 38));
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(9, 16, 9, 16));
        return btn;
    }

    private JButton buildLightButton(String text) {
        JButton btn = new JButton(text);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(true);
        btn.setOpaque(true);
        btn.setBackground(Color.WHITE);
        btn.setForeground(new Color(45, 45, 45));
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)),
                new EmptyBorder(9, 16, 9, 16)
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

                switch (status.toUpperCase()) {
                    case "ACCEPTED" -> label.setBackground(new Color(240, 205, 90));
                    case "PROCESSED" -> label.setBackground(new Color(200, 225, 110));
                    case "DISPATCHED" -> label.setBackground(new Color(140, 210, 255));
                    case "DELIVERED" -> label.setBackground(new Color(201, 224, 98));
                    default -> label.setBackground(Color.WHITE);
                }
            }

            return label;
        }
    }
}