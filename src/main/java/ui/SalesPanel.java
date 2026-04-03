
package ui;

import app.Session;
import database.CustomerDB;
import database.ProductDB;
import database.SaleDB;
import domain.Customer;
import domain.Product;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SalesPanel extends JPanel implements ThemeManager.ThemeListener {

    private final ScreenRouter router;

    private JPanel contentPanel;
    private JPanel leftCard;
    private JPanel rightCard;
    private JPanel topControls;
    private JPanel bottomControls;
    private JPanel centerSection;

    private JTable productsTable;
    private JTable cartTable;

    private JScrollPane productsScrollPane;
    private JScrollPane cartScrollPane;

    private JButton addToCartBtn;
    private JButton removeItemBtn;
    private JButton selectCustomerBtn;
    private JButton clearCartBtn;
    private JButton confirmSaleBtn;

    private JComboBox<String> filterCombo;
    private JComboBox<String> sortCombo;

    private JRadioButton accountHolderRadio;
    private JRadioButton occasionalCustomerRadio;

    private JLabel availableProductsLabel;
    private JLabel currentCartLabel;
    private JLabel saleTypeLabel;
    private JLabel customerSelectedLabel;
    private JLabel totalLabel;

    private DefaultTableModel productsModel;
    private DefaultTableModel cartModel;
    private final Map<Product, Integer> cart = new LinkedHashMap<>();
    private Customer selectedCustomer;

    public SalesPanel(ScreenRouter router) {
        this.router = router;

        setLayout(new BorderLayout());
        ThemeManager.register(this);

        AppShell shell = new AppShell(
                router,
                MainFrame.SCREEN_SALES,
                "Product Information",
                "Record and process sales",
                buildContent()
        );

        add(shell, BorderLayout.CENTER);
        wireActions();
        loadCatalogue();
        applyTheme();
    }

    private JPanel buildContent() {
        contentPanel = new JPanel(new BorderLayout(22, 22));
        contentPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        contentPanel.setOpaque(false);

        topControls = buildTopControls();

        centerSection = new JPanel(new GridLayout(1, 2, 22, 0));
        centerSection.setOpaque(false);

        leftCard = AppShell.createCard();
        leftCard.setLayout(new BorderLayout(14, 14));
        leftCard.setBorder(new EmptyBorder(20, 20, 20, 20));

        rightCard = AppShell.createCard();
        rightCard.setLayout(new BorderLayout(14, 14));
        rightCard.setBorder(new EmptyBorder(20, 20, 20, 20));

        availableProductsLabel = new JLabel("Available Products");
        availableProductsLabel.setFont(new Font("SansSerif", Font.BOLD, 18));

        currentCartLabel = new JLabel("Current Cart");
        currentCartLabel.setFont(new Font("SansSerif", Font.BOLD, 18));

        productsModel = new DefaultTableModel(
                new String[]{"Product ID", "Description", "Price £", "VAT %", "Stock"}, 0
        ) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        productsTable = new JTable(productsModel);

        cartModel = new DefaultTableModel(
                new String[]{"Description", "Qty", "Unit £", "Line Total £"}, 0
        ) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        cartTable = new JTable(cartModel);

        configureTable(productsTable);
        configureTable(cartTable);

        productsScrollPane = new JScrollPane(productsTable);
        cartScrollPane = new JScrollPane(cartTable);

        styleScrollPane(productsScrollPane);
        styleScrollPane(cartScrollPane);

        leftCard.add(availableProductsLabel, BorderLayout.NORTH);
        leftCard.add(productsScrollPane, BorderLayout.CENTER);

        JPanel rightInner = new JPanel(new BorderLayout(14, 14));
        rightInner.setOpaque(false);
        rightInner.add(currentCartLabel, BorderLayout.NORTH);
        rightInner.add(cartScrollPane, BorderLayout.CENTER);

        totalLabel = new JLabel("Total: £0.00");
        totalLabel.setFont(new Font("SansSerif", Font.BOLD, 16));

        JPanel totalWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        totalWrap.setOpaque(false);
        totalWrap.add(totalLabel);

        rightInner.add(totalWrap, BorderLayout.SOUTH);
        rightCard.add(rightInner, BorderLayout.CENTER);

        centerSection.add(leftCard);
        centerSection.add(rightCard);

        bottomControls = buildBottomControls();

        contentPanel.add(topControls, BorderLayout.NORTH);
        contentPanel.add(centerSection, BorderLayout.CENTER);
        contentPanel.add(bottomControls, BorderLayout.SOUTH);

        return contentPanel;
    }

    private JPanel buildTopControls() {
        JPanel panel = new JPanel(new BorderLayout(20, 0));
        panel.setOpaque(false);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        left.setOpaque(false);

        addToCartBtn = new JButton("+  Add To Cart");
        removeItemBtn = new JButton("Remove Item");

        setControlSize(addToCartBtn, 150, 42);
        setControlSize(removeItemBtn, 140, 42);

        left.add(addToCartBtn);
        left.add(removeItemBtn);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        right.setOpaque(false);

        filterCombo = new JComboBox<>(new String[]{"All Products", "In Stock", "Low Stock"});
        sortCombo = new JComboBox<>(new String[]{"Sort by: Name", "Sort by: Price", "Sort by: Stock"});

        setControlSize(filterCombo, 180, 42);
        setControlSize(sortCombo, 160, 42);

        right.add(filterCombo);
        right.add(sortCombo);

        panel.add(left, BorderLayout.WEST);
        panel.add(right, BorderLayout.EAST);

        return panel;
    }

    private JPanel buildBottomControls() {
        JPanel panel = new JPanel(new BorderLayout(20, 0));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(6, 0, 0, 0));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setOpaque(false);

        saleTypeLabel = new JLabel("Sale type:");

        accountHolderRadio = new JRadioButton("Account Holder");
        occasionalCustomerRadio = new JRadioButton("Occasional Customer");

        ButtonGroup group = new ButtonGroup();
        group.add(accountHolderRadio);
        group.add(occasionalCustomerRadio);
        occasionalCustomerRadio.setSelected(true);

        selectCustomerBtn = new JButton("Select Customer");
        setControlSize(selectCustomerBtn, 160, 42);

        customerSelectedLabel = new JLabel("No customer selected");

        left.add(saleTypeLabel);
        left.add(accountHolderRadio);
        left.add(occasionalCustomerRadio);
        left.add(Box.createHorizontalStrut(8));
        left.add(selectCustomerBtn);
        left.add(customerSelectedLabel);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        right.setOpaque(false);

        clearCartBtn = new JButton("Clear Cart");
        confirmSaleBtn = new JButton("Confirm Sale");

        setControlSize(clearCartBtn, 120, 42);
        setControlSize(confirmSaleBtn, 145, 42);

        right.add(clearCartBtn);
        right.add(confirmSaleBtn);

        panel.add(left, BorderLayout.WEST);
        panel.add(right, BorderLayout.EAST);

        return panel;
    }

    private void wireActions() {
        addToCartBtn.addActionListener(e -> addSelectedProductToCart());
        removeItemBtn.addActionListener(e -> removeSelectedCartItem());
        clearCartBtn.addActionListener(e -> clearCart());
        confirmSaleBtn.addActionListener(e -> confirmSale());

        filterCombo.addActionListener(e -> loadCatalogue());
        sortCombo.addActionListener(e -> loadCatalogue());

        accountHolderRadio.addActionListener(e -> {
            selectCustomerBtn.setEnabled(true);
            selectedCustomer = null;
            customerSelectedLabel.setText("No customer selected");
        });

        occasionalCustomerRadio.addActionListener(e -> {
            selectedCustomer = null;
            customerSelectedLabel.setText("No customer selected");
            selectCustomerBtn.setEnabled(false);
        });

        selectCustomerBtn.setEnabled(false);

        selectCustomerBtn.addActionListener(e -> {
            List<Customer> customers = CustomerDB.getAllActiveCustomers();
            if (customers.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No customers found.");
                return;
            }

            Customer chosen = (Customer) JOptionPane.showInputDialog(
                    this,
                    "Select account holder:",
                    "Customer",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    customers.toArray(),
                    customers.get(0)
            );

            if (chosen != null) {
                if (!chosen.isActive()) {
                    JOptionPane.showMessageDialog(this, "The selected account is " + chosen.getAccountStatus() + ".");
                    return;
                }
                selectedCustomer = chosen;
                customerSelectedLabel.setText(chosen.getFullName() + " | Balance £" +
                        String.format("%.2f", chosen.getCurrentBalance()));
            }
        });

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

        String filter = (String) filterCombo.getSelectedItem();
        String sort = (String) sortCombo.getSelectedItem();

        products.removeIf(p ->
                "In Stock".equals(filter) && p.getStockQuantity() <= 0 ||
                "Low Stock".equals(filter) && !p.isLowStock()
        );

        if ("Sort by: Price".equals(sort)) {
            products.sort((a, b) -> Double.compare(a.getPrice(), b.getPrice()));
        } else if ("Sort by: Stock".equals(sort)) {
            products.sort((a, b) -> Integer.compare(b.getStockQuantity(), a.getStockQuantity()));
        } else {
            products.sort((a, b) -> a.getDescription().compareToIgnoreCase(b.getDescription()));
        }

        for (Product p : products) {
            productsModel.addRow(new Object[]{
                    p.getProductId(),
                    p.getDescription(),
                    String.format("%.2f", p.getPrice()),
                    String.format("%.2f", p.getVatRate()),
                    p.getStockQuantity()
            });
        }
    }

    private void addSelectedProductToCart() {
        int row = productsTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a product.");
            return;
        }

        int productId = (int) productsModel.getValueAt(row, 0);
        Product product = ProductDB.getById(productId);
        if (product == null) {
            JOptionPane.showMessageDialog(this, "Could not load the selected product.");
            return;
        }

        String input = JOptionPane.showInputDialog(this, "Quantity:");
        if (input == null || input.trim().isEmpty()) return;

        try {
            int qty = Integer.parseInt(input.trim());
            if (qty <= 0) throw new NumberFormatException();
            if (qty > product.getStockQuantity()) {
                JOptionPane.showMessageDialog(this, "Not enough stock. Available: " + product.getStockQuantity());
                return;
            }

            cart.merge(product, qty, Integer::sum);
            refreshCart();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid positive quantity.");
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
        double total = 0.0;

        for (Map.Entry<Product, Integer> entry : cart.entrySet()) {
            Product p = entry.getKey();
            int qty = entry.getValue();
            double lineTotal = p.getPrice() * qty * (1 + p.getVatRate() / 100.0);
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
        selectedCustomer = null;
        customerSelectedLabel.setText("No customer selected");
        occasionalCustomerRadio.setSelected(true);
        selectCustomerBtn.setEnabled(false);
        refreshCart();
    }

    private void confirmSale() {
        if (cart.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Cart is empty.");
            return;
        }

        boolean accountSale = accountHolderRadio.isSelected();
        if (accountSale && selectedCustomer == null) {
            JOptionPane.showMessageDialog(this, "Please select an account holder.");
            return;
        }

        JComboBox<String> paymentMethodCombo = new JComboBox<>(
                accountSale ? new String[]{"CARD", "CREDIT_ACCOUNT"} : new String[]{"CASH", "CARD"}
        );

        int choice = JOptionPane.showConfirmDialog(
                this,
                new Object[]{"Payment Method:", paymentMethodCombo},
                "Confirm Sale",
                JOptionPane.OK_CANCEL_OPTION
        );
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }

        String saleType = accountSale ? "ACCOUNT" : "OCCASIONAL";
        String paymentMethod = (String) paymentMethodCombo.getSelectedItem();

        int saleId = SaleDB.recordSale(
                Session.getUserId(),
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

        String documentNumber;
        if (accountSale) {
            double total = cart.entrySet().stream()
                    .mapToDouble(e -> e.getKey().getPrice() * e.getValue() * (1 + e.getKey().getVatRate() / 100.0))
                    .sum();

            documentNumber = SaleDB.generateInvoice(saleId, selectedCustomer.getCustomerId(), total);
            JOptionPane.showMessageDialog(this,
                    "Sale recorded.\nInvoice: " + documentNumber + "\nCustomer: " + selectedCustomer.getFullName());
        } else {
            documentNumber = SaleDB.generateReceipt(saleId);
            JOptionPane.showMessageDialog(this, "Sale recorded.\nReceipt: " + documentNumber);
        }

        clearCart();
        loadCatalogue();
    }

    private void setControlSize(JComponent component, int width, int height) {
        Dimension size = new Dimension(width, height);
        component.setPreferredSize(size);
        component.setMinimumSize(size);
    }

    private void configureTable(JTable table) {
        table.setRowHeight(52);
        table.setShowGrid(true);
        table.setIntercellSpacing(new Dimension(1, 1));
        table.setFillsViewportHeight(true);
        table.setBorder(BorderFactory.createEmptyBorder());
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setDefaultEditor(Object.class, null);
        table.setFont(new Font("SansSerif", Font.PLAIN, 13));

        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setFont(new Font("SansSerif", Font.BOLD, 14));
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 34));
        header.setBorder(BorderFactory.createEmptyBorder());

        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.setHorizontalAlignment(SwingConstants.LEFT);
        renderer.setBorder(new EmptyBorder(0, 12, 0, 12));
        table.setDefaultRenderer(Object.class, renderer);
    }

    private void styleScrollPane(JScrollPane sp) {
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setOpaque(true);
        sp.setBackground(ThemeManager.tableBackground());
        sp.getViewport().setBackground(ThemeManager.tableBackground());
        sp.getViewport().setBorder(null);

        if (sp.getVerticalScrollBar() != null) {
            sp.getVerticalScrollBar().setBackground(ThemeManager.panelBackground());
            sp.getVerticalScrollBar().setBorder(BorderFactory.createEmptyBorder());
        }
        if (sp.getHorizontalScrollBar() != null) {
            sp.getHorizontalScrollBar().setBackground(ThemeManager.panelBackground());
            sp.getHorizontalScrollBar().setBorder(BorderFactory.createEmptyBorder());
        }
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
        renderer.setBorder(new EmptyBorder(0, 12, 0, 12));
        table.setDefaultRenderer(Object.class, renderer);
    }

    private void stylePrimaryButton(JButton button) {
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setFont(new Font("SansSerif", Font.BOLD, 14));
        button.setBackground(ThemeManager.buttonDark());
        button.setForeground(ThemeManager.textLight());
        button.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private void styleSecondaryButton(JButton button) {
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setFont(new Font("SansSerif", Font.BOLD, 14));
        button.setBackground(ThemeManager.buttonLight());
        button.setForeground(ThemeManager.textPrimary());
        button.setBorder(BorderFactory.createLineBorder(ThemeManager.borderColor()));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private void styleCombo(JComboBox<String> comboBox) {
        comboBox.setFocusable(false);
        comboBox.setOpaque(true);
        comboBox.setBackground(ThemeManager.comboBackground());
        comboBox.setForeground(ThemeManager.comboForeground());
        comboBox.setFont(new Font("SansSerif", Font.PLAIN, 14));
        comboBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.borderColor()),
                new EmptyBorder(0, 8, 0, 8)
        ));
    }

    private void styleRadio(JRadioButton radioButton) {
        radioButton.setOpaque(false);
        radioButton.setForeground(ThemeManager.textPrimary());
        radioButton.setFont(new Font("SansSerif", Font.PLAIN, 14));
        radioButton.setFocusPainted(false);
        radioButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    @Override
    public void applyTheme() {
        setBackground(ThemeManager.appBackground());

        if (contentPanel != null) contentPanel.setBackground(ThemeManager.appBackground());
        if (centerSection != null) centerSection.setBackground(ThemeManager.appBackground());
        if (leftCard != null) leftCard.setBackground(ThemeManager.panelBackground());
        if (rightCard != null) rightCard.setBackground(ThemeManager.panelBackground());
        if (topControls != null) topControls.setBackground(ThemeManager.appBackground());
        if (bottomControls != null) bottomControls.setBackground(ThemeManager.appBackground());

        if (availableProductsLabel != null) availableProductsLabel.setForeground(ThemeManager.textPrimary());
        if (currentCartLabel != null) currentCartLabel.setForeground(ThemeManager.textPrimary());
        if (saleTypeLabel != null) saleTypeLabel.setForeground(ThemeManager.textPrimary());
        if (customerSelectedLabel != null) customerSelectedLabel.setForeground(ThemeManager.textSecondary());
        if (totalLabel != null) totalLabel.setForeground(ThemeManager.textPrimary());

        if (productsTable != null) applyTableTheme(productsTable);
        if (cartTable != null) applyTableTheme(cartTable);

        if (productsScrollPane != null) styleScrollPane(productsScrollPane);
        if (cartScrollPane != null) styleScrollPane(cartScrollPane);

        if (addToCartBtn != null) stylePrimaryButton(addToCartBtn);
        if (confirmSaleBtn != null) stylePrimaryButton(confirmSaleBtn);

        if (removeItemBtn != null) styleSecondaryButton(removeItemBtn);
        if (selectCustomerBtn != null) styleSecondaryButton(selectCustomerBtn);
        if (clearCartBtn != null) styleSecondaryButton(clearCartBtn);

        if (filterCombo != null) styleCombo(filterCombo);
        if (sortCombo != null) styleCombo(sortCombo);

        if (accountHolderRadio != null) styleRadio(accountHolderRadio);
        if (occasionalCustomerRadio != null) styleRadio(occasionalCustomerRadio);

        repaint();
        revalidate();
    }
}
