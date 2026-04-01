package ui;

import app.Session;
import database.CustomerDB;
import database.ProductDB;
import database.SaleDB;
import domain.Customer;
import domain.Product;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SalesPanel extends JPanel {

    private DefaultTableModel productModel;
    private DefaultTableModel cartModel;

    private JTable productTable;
    private JTable cartTable;

    private final Map<Product, Integer> cart = new LinkedHashMap<>();

    private JLabel customerLabel;
    private JLabel totalLabel;
    private Customer selectedCustomer = null;

    private JRadioButton accountBtn;
    private JRadioButton occasionalBtn;
    private JComboBox<String> paymentCombo;

    public SalesPanel(ScreenRouter router) {
        setLayout(new BorderLayout());
        setBackground(new Color(242, 242, 242));

        JPanel salesContent = buildSalesContent(router);

        AppShell shell = new AppShell(
                router,
                MainFrame.SCREEN_SALES,
                "Product Information",
                "Record and process sales",
                salesContent
        );

        add(shell, BorderLayout.CENTER);

        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                loadCatalogue();
                clearCart();
            }
        });
    }

    private JPanel buildSalesContent(ScreenRouter router) {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setOpaque(false);

        JPanel card = AppShell.createCard();
        card.setLayout(new BorderLayout(0, 18));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(232, 232, 232)),
                new EmptyBorder(20, 20, 20, 20)
        ));

        card.add(buildTopControls(), BorderLayout.NORTH);
        card.add(buildTablesSection(), BorderLayout.CENTER);
        card.add(buildBottomControls(), BorderLayout.SOUTH);

        outer.add(card, BorderLayout.CENTER);
        return outer;
    }

    private JPanel buildTopControls() {
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);

        JButton addToCartBtn = buildDarkButton("+  Add To Cart");
        addToCartBtn.addActionListener(e -> addSelectedProductToCart());

        JButton removeBtn = buildLightButton("Remove Item");
        removeBtn.addActionListener(e -> removeSelectedCartItem());

        left.add(addToCartBtn);
        left.add(removeBtn);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);

        JComboBox<String> fakeFilter = new JComboBox<>(new String[]{
                "All Transactions", "Current Cart", "Ready to Confirm"
        });
        JComboBox<String> fakeSort = new JComboBox<>(new String[]{
                "Sort by: Date", "Sort by: Product", "Sort by: Amount"
        });

        styleCombo(fakeFilter);
        styleCombo(fakeSort);

        right.add(fakeFilter);
        right.add(fakeSort);

        top.add(left, BorderLayout.WEST);
        top.add(right, BorderLayout.EAST);

        return top;
    }

    private JPanel buildTablesSection() {
        JPanel center = new JPanel(new GridLayout(1, 2, 18, 18));
        center.setOpaque(false);

        center.add(buildCataloguePanel());
        center.add(buildCartPanel());

        return center;
    }

    private JPanel buildCataloguePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(235, 235, 235)),
                new EmptyBorder(12, 12, 12, 12)
        ));
        panel.setBackground(Color.WHITE);

        JLabel title = new JLabel("Available Products");
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        title.setForeground(new Color(35, 35, 35));

        String[] prodCols = {"Product ID", "Description", "Price £", "VAT %", "Stock"};
        productModel = new DefaultTableModel(prodCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        productTable = new JTable(productModel);
        productTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        productTable.setRowHeight(38);
        productTable.setShowGrid(false);
        productTable.setIntercellSpacing(new Dimension(0, 0));
        productTable.setFont(new Font("SansSerif", Font.PLAIN, 12));
        productTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        productTable.getTableHeader().setReorderingAllowed(false);

        JScrollPane scrollPane = new JScrollPane(productTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        panel.add(title, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel buildCartPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(235, 235, 235)),
                new EmptyBorder(12, 12, 12, 12)
        ));
        panel.setBackground(Color.WHITE);

        JLabel title = new JLabel("Current Cart");
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        title.setForeground(new Color(35, 35, 35));

        String[] cartCols = {"Description", "Qty", "Unit £", "Line Total £"};
        cartModel = new DefaultTableModel(cartCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        cartTable = new JTable(cartModel);
        cartTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        cartTable.setRowHeight(38);
        cartTable.setShowGrid(false);
        cartTable.setIntercellSpacing(new Dimension(0, 0));
        cartTable.setFont(new Font("SansSerif", Font.PLAIN, 12));
        cartTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        cartTable.getTableHeader().setReorderingAllowed(false);

        totalLabel = new JLabel("Total: £0.00");
        totalLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        totalLabel.setForeground(new Color(35, 35, 35));
        totalLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        JScrollPane scrollPane = new JScrollPane(cartTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        panel.add(title, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(totalLabel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel buildBottomControls() {
        JPanel bottom = new JPanel(new BorderLayout(18, 0));
        bottom.setOpaque(false);
        bottom.setBorder(new EmptyBorder(8, 0, 0, 0));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setOpaque(false);

        accountBtn = new JRadioButton("Account Holder");
        occasionalBtn = new JRadioButton("Occasional Customer", true);

        styleRadio(accountBtn);
        styleRadio(occasionalBtn);

        ButtonGroup typeGroup = new ButtonGroup();
        typeGroup.add(accountBtn);
        typeGroup.add(occasionalBtn);

        JButton selectCustomerBtn = buildLightButton("Select Customer");
        selectCustomerBtn.setEnabled(false);

        customerLabel = new JLabel("No customer selected");
        customerLabel.setForeground(new Color(120, 120, 120));
        customerLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));

        paymentCombo = new JComboBox<>(new String[]{"CASH", "CARD"});
        styleCombo(paymentCombo);

        left.add(new JLabel("Sale type:"));
        left.add(accountBtn);
        left.add(occasionalBtn);
        left.add(selectCustomerBtn);
        left.add(customerLabel);
        left.add(new JLabel("Payment:"));
        left.add(paymentCombo);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);

        JButton clearBtn = buildLightButton("Clear Cart");
        JButton confirmBtn = buildDarkButton("Confirm Sale");

        clearBtn.addActionListener(e -> clearCart());
        confirmBtn.addActionListener(e -> confirmSale());

        right.add(clearBtn);
        right.add(confirmBtn);

        bottom.add(left, BorderLayout.CENTER);
        bottom.add(right, BorderLayout.EAST);

        accountBtn.addActionListener(e -> {
            paymentCombo.setModel(new DefaultComboBoxModel<>(new String[]{"CARD", "CREDIT_ACCOUNT"}));
            styleCombo(paymentCombo);
            selectCustomerBtn.setEnabled(true);
        });

        occasionalBtn.addActionListener(e -> {
            paymentCombo.setModel(new DefaultComboBoxModel<>(new String[]{"CASH", "CARD"}));
            styleCombo(paymentCombo);
            selectedCustomer = null;
            customerLabel.setText("No customer selected");
            selectCustomerBtn.setEnabled(false);
        });

        selectCustomerBtn.addActionListener(e -> {
            List<Customer> customers = CustomerDB.getAllActiveCustomers();
            if (customers.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No customers found.");
                return;
            }

            Customer[] arr = customers.toArray(new Customer[0]);
            Customer chosen = (Customer) JOptionPane.showInputDialog(
                    this,
                    "Select account holder:",
                    "Customer",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    arr,
                    arr[0]
            );

            if (chosen != null) {
                if (!chosen.isActive()) {
                    JOptionPane.showMessageDialog(
                            this,
                            "Account is " + chosen.getAccountStatus() + ". Cannot process sale."
                    );
                    return;
                }

                selectedCustomer = chosen;
                customerLabel.setText(
                        chosen.getFullName()
                                + " | Balance: £" + String.format("%.2f", chosen.getCurrentBalance())
                                + " / Limit: £" + String.format("%.2f", chosen.getCreditLimit())
                );
            }
        });

        return bottom;
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

    private void styleRadio(JRadioButton radio) {
        radio.setOpaque(false);
        radio.setFont(new Font("SansSerif", Font.PLAIN, 12));
        radio.setForeground(new Color(45, 45, 45));
        radio.setFocusPainted(false);
    }

    private void loadCatalogue() {
        if (productModel == null) {
            return;
        }

        productModel.setRowCount(0);
        for (Product p : ProductDB.getAllProducts()) {
            productModel.addRow(new Object[]{
                    p.getProductId(),
                    p.getDescription(),
                    String.format("%.2f", p.getPrice()),
                    String.format("%.2f", p.getVatRate()),
                    p.getStockQuantity()
            });
        }
    }

    private void addSelectedProductToCart() {
        int row = productTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a product.");
            return;
        }

        int productId = (int) productModel.getValueAt(row, 0);
        Product product = ProductDB.getAllProducts().stream()
                .filter(p -> p.getProductId() == productId)
                .findFirst()
                .orElse(null);

        if (product == null) {
            return;
        }

        String input = JOptionPane.showInputDialog(this, "Quantity:");
        if (input == null || input.trim().isEmpty()) {
            return;
        }

        try {
            int qty = Integer.parseInt(input.trim());
            if (qty <= 0) {
                throw new NumberFormatException();
            }

            if (qty > product.getStockQuantity()) {
                JOptionPane.showMessageDialog(
                        this,
                        "Not enough stock. Available: " + product.getStockQuantity()
                );
                return;
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
        cartModel.setRowCount(0);

        double total = 0;
        for (Map.Entry<Product, Integer> entry : cart.entrySet()) {
            Product p = entry.getKey();
            int qty = entry.getValue();

            double lineTotal = p.getPrice() * qty * (1 + p.getVatRate() / 100);
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

        if (cartModel != null) {
            cartModel.setRowCount(0);
        }

        if (totalLabel != null) {
            totalLabel.setText("Total: £0.00");
        }

        selectedCustomer = null;

        if (customerLabel != null) {
            customerLabel.setText("No customer selected");
        }

        if (occasionalBtn != null) {
            occasionalBtn.setSelected(true);
        }

        if (paymentCombo != null) {
            paymentCombo.setModel(new DefaultComboBoxModel<>(new String[]{"CASH", "CARD"}));
            styleCombo(paymentCombo);
        }
    }

    private void confirmSale() {
        if (cart.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Cart is empty.");
            return;
        }

        boolean isAccount = accountBtn.isSelected();

        if (isAccount && selectedCustomer == null) {
            JOptionPane.showMessageDialog(this, "Please select an account holder.");
            return;
        }

        String saleType = isAccount ? "ACCOUNT" : "OCCASIONAL";
        String paymentMethod = (String) paymentCombo.getSelectedItem();
        int userId = Session.getCurrentUser().getUserId();

        int saleId = SaleDB.recordSale(
                userId,
                selectedCustomer,
                saleType,
                paymentMethod,
                cart,
                0.00
        );

        if (saleId == -1) {
            JOptionPane.showMessageDialog(this, "Sale failed. Please try again.");
            return;
        }

        String docNumber;
        if (isAccount) {
            double total = cart.entrySet().stream()
                    .mapToDouble(e -> e.getKey().getPrice() * e.getValue()
                            * (1 + e.getKey().getVatRate() / 100))
                    .sum();

            docNumber = SaleDB.generateInvoice(
                    saleId,
                    selectedCustomer.getCustomerId(),
                    total
            );

            JOptionPane.showMessageDialog(
                    this,
                    "Sale recorded.\nInvoice: " + docNumber
                            + "\nCustomer: " + selectedCustomer.getFullName()
            );
        } else {
            docNumber = SaleDB.generateReceipt(saleId);

            JOptionPane.showMessageDialog(
                    this,
                    "Sale recorded.\nReceipt: " + docNumber
            );
        }

        loadCatalogue();
        clearCart();
    }
}