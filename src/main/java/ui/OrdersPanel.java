package ui;

import database.ProductDB;
import database.RestockOrderDB;
import domain.Product;
import domain.RestockOrder;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OrdersPanel extends JPanel {

    private DefaultTableModel ordersModel;
    private DefaultTableModel cartModel;
    private Map<Product, Integer> cart = new LinkedHashMap<>();
    private JLabel totalLabel;

    // status progression as per brief
    private static final String[] STATUS_FLOW = {"ACCEPTED", "PROCESSED", "DISPATCHED", "DELIVERED"};

    public OrdersPanel(ScreenRouter router) {
        setLayout(new BorderLayout());

        // --- HEADER ---
        JPanel header = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Restock Orders", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 28));
        JButton backBtn = new JButton("Back");
        backBtn.addActionListener(e -> router.goTo(MainFrame.SCREEN_DASHBOARD));
        header.add(backBtn, BorderLayout.WEST);
        header.add(title, BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        // --- TABBED PANE ---
        JTabbedPane tabs = new JTabbedPane();

        // TAB 1 — place new order
        tabs.addTab("Place New Order", buildNewOrderTab());

        // TAB 2 — view existing orders
        tabs.addTab("Order History", buildOrderHistoryTab());

        add(tabs, BorderLayout.CENTER);

        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                loadOrderHistory();
            }
        });
    }

    // ----------------------------------------------------------------
    // TAB 1 — new order
    // ----------------------------------------------------------------
    private JPanel buildNewOrderTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // LEFT — catalogue
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createTitledBorder("Select Products"));

        String[] prodCols = {"ID", "Item ID", "Description", "Price £", "Stock", "Min Stock"};
        DefaultTableModel prodModel = new DefaultTableModel(prodCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable prodTable = new JTable(prodModel);
        prodTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JButton addToCartBtn = new JButton("Add to Order");
        leftPanel.add(new JScrollPane(prodTable), BorderLayout.CENTER);
        leftPanel.add(addToCartBtn, BorderLayout.SOUTH);

        // RIGHT — order cart
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createTitledBorder("Order Summary"));

        String[] cartCols = {"Description", "Qty", "Unit Cost £", "Line Total £"};
        cartModel = new DefaultTableModel(cartCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable cartTable = new JTable(cartModel);

        totalLabel = new JLabel("Total: £0.00", SwingConstants.RIGHT);
        totalLabel.setFont(new Font("SansSerif", Font.BOLD, 14));

        JButton removeBtn  = new JButton("Remove");
        JPanel cartBottom  = new JPanel(new BorderLayout());
        cartBottom.add(removeBtn, BorderLayout.WEST);
        cartBottom.add(totalLabel, BorderLayout.EAST);

        rightPanel.add(new JScrollPane(cartTable), BorderLayout.CENTER);
        rightPanel.add(cartBottom, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        split.setDividerLocation(480);
        panel.add(split, BorderLayout.CENTER);

        // BOTTOM — merchant ID + confirm
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        bottom.add(new JLabel("Merchant ID:"));
        JTextField merchantField = new JTextField(15);
        merchantField.setText("MERCHANT-001");
        JButton placeBtn = new JButton("Place Order");
        placeBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        bottom.add(merchantField);
        bottom.add(placeBtn);
        panel.add(bottom, BorderLayout.SOUTH);

        // load catalogue
        panel.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                loadCatalogue(prodModel);
            }
        });

        // add to cart
        addToCartBtn.addActionListener(e -> {
            int row = prodTable.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(panel, "Please select a product.");
                return;
            }
            int productId = (int) prodModel.getValueAt(row, 0);
            Product product = ProductDB.getAllProducts().stream()
                    .filter(p -> p.getProductId() == productId)
                    .findFirst().orElse(null);
            if (product == null) return;

            String input = JOptionPane.showInputDialog(panel, "Quantity to order:");
            if (input == null || input.trim().isEmpty()) return;
            try {
                int qty = Integer.parseInt(input.trim());
                if (qty <= 0) throw new NumberFormatException();
                cart.merge(product, qty, Integer::sum);
                refreshCart();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(panel, "Please enter a valid quantity.");
            }
        });

        // remove from cart
        removeBtn.addActionListener(e -> {
            int row = cartTable.getSelectedRow();
            if (row == -1) return;
            Product key = (Product) cart.keySet().toArray()[row];
            cart.remove(key);
            refreshCart();
        });

        // place order
        placeBtn.addActionListener(e -> {
            if (cart.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "Order is empty.");
                return;
            }
            String merchantId = merchantField.getText().trim();
            if (merchantId.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "Please enter a merchant ID.");
                return;
            }

            String orderNumber = RestockOrderDB.placeOrder(merchantId, cart);
            if (orderNumber != null) {
                JOptionPane.showMessageDialog(panel,
                        "Order placed successfully.\nOrder number: " + orderNumber);
                cart.clear();
                refreshCart();
                loadOrderHistory();
            } else {
                JOptionPane.showMessageDialog(panel, "Failed to place order. Please try again.");
            }
        });

        return panel;
    }

    // ----------------------------------------------------------------
    // TAB 2 — order history
    // ----------------------------------------------------------------
    private JPanel buildOrderHistoryTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] cols = {"ID", "Order Number", "Merchant ID", "Status", "Total £", "Created At"};
        ordersModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable ordersTable = new JTable(ordersModel);
        ordersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ordersTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));

        panel.add(new JScrollPane(ordersTable), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 8));
        JButton updateBtn  = new JButton("Update Status");
        JButton refreshBtn = new JButton("Refresh");
        buttons.add(updateBtn);
        buttons.add(refreshBtn);
        panel.add(buttons, BorderLayout.SOUTH);

        refreshBtn.addActionListener(e -> loadOrderHistory());

        // update status — follows ACCEPTED → PROCESSED → DISPATCHED → DELIVERED
        updateBtn.addActionListener(e -> {
            int row = ordersTable.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(panel, "Please select an order.");
                return;
            }

            int    orderId   = (int) ordersModel.getValueAt(row, 0);
            String curStatus = (String) ordersModel.getValueAt(row, 3);

            String nextStatus = getNextStatus(curStatus);
            if (nextStatus == null) {
                JOptionPane.showMessageDialog(panel, "This order is already delivered.");
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(panel,
                    "Update status from " + curStatus + " to " + nextStatus + "?",
                    "Confirm", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                RestockOrderDB.updateStatus(orderId, curStatus, nextStatus);
                loadOrderHistory();
            }
        });

        return panel;
    }

    private void loadCatalogue(DefaultTableModel model) {
        model.setRowCount(0);
        for (Product p : ProductDB.getAllProducts()) {
            model.addRow(new Object[]{
                    p.getProductId(),
                    p.getItemId(),
                    p.getDescription(),
                    String.format("%.2f", p.getPrice()),
                    p.getStockQuantity(),
                    p.getMinStockLevel()
            });
        }
    }

    private void loadOrderHistory() {
        if (ordersModel == null) return;
        ordersModel.setRowCount(0);
        for (RestockOrder o : RestockOrderDB.getAllOrders()) {
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

    private void refreshCart() {
        cartModel.setRowCount(0);
        double total = 0;
        for (Map.Entry<Product, Integer> entry : cart.entrySet()) {
            Product p   = entry.getKey();
            int     qty = entry.getValue();
            double  lineTotal = p.getPrice() * qty;
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

    private String getNextStatus(String current) {
        for (int i = 0; i < STATUS_FLOW.length - 1; i++) {
            if (STATUS_FLOW[i].equals(current)) return STATUS_FLOW[i + 1];
        }
        return null;
    }
}