
package ui;

import app.Session;
import database.CustomerDB;
import database.ProductDB;
import database.SaleDB;
import domain.Customer;
import domain.Product;
import integration.PUApiClient;

import java.util.ArrayList;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
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
    private JPanel centerSection;
    private JPanel checkoutPanel;
    private JPanel customerRow;

    private JTable productsTable;
    private JTable cartTable;
    private JScrollPane productsScrollPane;
    private JScrollPane cartScrollPane;

    private JButton addToCartBtn;
    private JButton removeItemBtn;
    private JButton clearCartBtn;
    private JButton selectCustomerBtn;
    private JButton confirmSaleBtn;

    private JComboBox<String> filterCombo;
    private JComboBox<String> sortCombo;
    private JComboBox<String> paymentCombo;

    private JRadioButton accountHolderRadio;
    private JRadioButton occasionalCustomerRadio;

    private JLabel availableProductsLabel;
    private JLabel currentCartLabel;
    private JLabel customerSelectedLabel;
    private JLabel totalLabel;

    private JTextField searchField;

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
                "Sales",
                "Record and process sales",
                buildContent()
        );

        add(shell, BorderLayout.CENTER);
        wireActions();
        loadCatalogue();
        applyTheme();
    }

    // ── Layout ────────────────────────────────────────────────────────────────

    private JPanel buildContent() {
        contentPanel = new JPanel(new BorderLayout(16, 0));
        contentPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        contentPanel.setOpaque(false);

        centerSection = new JPanel(new GridLayout(1, 2, 16, 0));
        centerSection.setOpaque(false);
        centerSection.add(buildLeftPanel());
        centerSection.add(buildRightPanel());

        contentPanel.add(centerSection, BorderLayout.CENTER);
        return contentPanel;
    }

    /** Left: search/filter controls + product catalogue + Add to Cart */
    private JPanel buildLeftPanel() {
        leftCard = AppShell.createCard();
        leftCard.setLayout(new BorderLayout(0, 10));
        leftCard.setBorder(new EmptyBorder(18, 18, 18, 18));

        // ── Header row ────────────────────────────────────────────────────────
        availableProductsLabel = new JLabel("Products");
        availableProductsLabel.setFont(new Font("SansSerif", Font.BOLD, 16));

        // ── Search field ──────────────────────────────────────────────────────
        searchField = new JTextField("Search products...") {
            @Override
            public void addNotify() {
                super.addNotify();
                setForeground(ThemeManager.textSecondary());
                addFocusListener(new java.awt.event.FocusAdapter() {
                    @Override public void focusGained(java.awt.event.FocusEvent e) {
                        if (getText().equals("Search products...")) {
                            setText("");
                            setForeground(ThemeManager.textPrimary());
                        }
                    }
                    @Override public void focusLost(java.awt.event.FocusEvent e) {
                        if (getText().trim().isEmpty()) {
                            setText("Search products...");
                            setForeground(ThemeManager.textSecondary());
                        }
                    }
                });
            }
        };
        searchField.setPreferredSize(new Dimension(0, 38));
        searchField.setFont(new Font("SansSerif", Font.PLAIN, 13));

        // ── Filter / sort row ─────────────────────────────────────────────────
        filterCombo = new JComboBox<>(new String[]{"All Products", "In Stock", "Low Stock"});
        sortCombo   = new JComboBox<>(new String[]{"Sort by: Name", "Sort by: Price", "Sort by: Stock"});

        JPanel filterRow = new JPanel(new GridLayout(1, 2, 8, 0));
        filterRow.setOpaque(false);
        filterRow.add(filterCombo);
        filterRow.add(sortCombo);

        // ── Controls wrapper ──────────────────────────────────────────────────
        JPanel topControls = new JPanel(new BorderLayout(0, 8));
        topControls.setOpaque(false);
        topControls.add(availableProductsLabel, BorderLayout.NORTH);
        topControls.add(searchField,            BorderLayout.CENTER);
        topControls.add(filterRow,              BorderLayout.SOUTH);

        // ── Products table (Product ID hidden — used only for lookup) ─────────
        productsModel = new DefaultTableModel(
                new String[]{"ID", "Description", "Price £", "Stock"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        productsTable = new JTable(productsModel);
        // Hide the internal ID column — still accessible via getValueAt(row, 0)
        productsTable.getColumnModel().getColumn(0).setMinWidth(0);
        productsTable.getColumnModel().getColumn(0).setMaxWidth(0);
        productsTable.getColumnModel().getColumn(0).setWidth(0);
        configureTable(productsTable);

        productsScrollPane = new JScrollPane(productsTable);
        styleScrollPane(productsScrollPane);

        // ── Add to Cart button (bottom of catalogue) ──────────────────────────
        addToCartBtn = new JButton("＋   Add to Cart");
        addToCartBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        addToCartBtn.setPreferredSize(new Dimension(0, 44));
        addToCartBtn.setFont(new Font("SansSerif", Font.BOLD, 14));

        leftCard.add(topControls,          BorderLayout.NORTH);
        leftCard.add(productsScrollPane,   BorderLayout.CENTER);
        leftCard.add(addToCartBtn,         BorderLayout.SOUTH);
        return leftCard;
    }

    /** Right: cart table + cart actions + checkout section */
    private JPanel buildRightPanel() {
        rightCard = AppShell.createCard();
        rightCard.setLayout(new BorderLayout(0, 10));
        rightCard.setBorder(new EmptyBorder(18, 18, 18, 18));

        // ── Cart header ───────────────────────────────────────────────────────
        currentCartLabel = new JLabel("Cart");
        currentCartLabel.setFont(new Font("SansSerif", Font.BOLD, 16));

        // ── Cart table ────────────────────────────────────────────────────────
        cartModel = new DefaultTableModel(
                new String[]{"Description", "Qty", "Unit £", "Line Total £"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        cartTable = new JTable(cartModel);
        configureTable(cartTable);

        cartScrollPane = new JScrollPane(cartTable);
        styleScrollPane(cartScrollPane);

        // ── Cart action buttons (below table) ─────────────────────────────────
        removeItemBtn = new JButton("Remove Selected");
        clearCartBtn  = new JButton("Clear Cart");

        JPanel cartActions = new JPanel(new BorderLayout(8, 0));
        cartActions.setOpaque(false);
        cartActions.add(removeItemBtn, BorderLayout.WEST);
        cartActions.add(clearCartBtn,  BorderLayout.EAST);

        // ── Cart section wrapper ──────────────────────────────────────────────
        JPanel cartSection = new JPanel(new BorderLayout(0, 8));
        cartSection.setOpaque(false);
        cartSection.add(currentCartLabel, BorderLayout.NORTH);
        cartSection.add(cartScrollPane,   BorderLayout.CENTER);
        cartSection.add(cartActions,      BorderLayout.SOUTH);

        rightCard.add(cartSection,        BorderLayout.CENTER);
        rightCard.add(buildCheckoutPanel(), BorderLayout.SOUTH);
        return rightCard;
    }

    /**
     * Checkout section: sale type → customer selector → payment method
     * → running total → Complete Sale button.
     */
    private JPanel buildCheckoutPanel() {
        checkoutPanel = new JPanel();
        checkoutPanel.setOpaque(false);
        checkoutPanel.setLayout(new BoxLayout(checkoutPanel, BoxLayout.Y_AXIS));
        checkoutPanel.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(1, 0, 0, 0, ThemeManager.borderColor()),
                new EmptyBorder(14, 0, 0, 0)
        ));

        // Sale type radios
        accountHolderRadio     = new JRadioButton("Account Holder");
        occasionalCustomerRadio = new JRadioButton("Occasional");
        occasionalCustomerRadio.setSelected(true);
        ButtonGroup group = new ButtonGroup();
        group.add(accountHolderRadio);
        group.add(occasionalCustomerRadio);

        JPanel saleTypeRow = makeRow();
        saleTypeRow.add(new JLabel("Sale type:"));
        saleTypeRow.add(Box.createHorizontalStrut(10));
        saleTypeRow.add(accountHolderRadio);
        saleTypeRow.add(Box.createHorizontalStrut(6));
        saleTypeRow.add(occasionalCustomerRadio);

        // Customer selector (shown only when Account Holder selected)
        selectCustomerBtn     = new JButton("Select Customer");
        customerSelectedLabel = new JLabel("No customer selected");
        customerSelectedLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));

        customerRow = makeRow();
        customerRow.add(selectCustomerBtn);
        customerRow.add(Box.createHorizontalStrut(10));
        customerRow.add(customerSelectedLabel);
        customerRow.setVisible(false);

        // Payment method
        paymentCombo = new JComboBox<>(new String[]{"CASH", "CARD"});
        paymentCombo.setAlignmentX(LEFT_ALIGNMENT);
        paymentCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        JPanel paymentRow = makeRow();
        paymentRow.add(new JLabel("Payment:"));
        paymentRow.add(Box.createHorizontalStrut(10));
        paymentRow.add(paymentCombo);

        // Total
        totalLabel = new JLabel("Total: £0.00");
        totalLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        totalLabel.setAlignmentX(LEFT_ALIGNMENT);

        // Complete Sale button
        confirmSaleBtn = new JButton("✓   Complete Sale");
        confirmSaleBtn.setAlignmentX(LEFT_ALIGNMENT);
        confirmSaleBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        confirmSaleBtn.setPreferredSize(new Dimension(0, 48));
        confirmSaleBtn.setFont(new Font("SansSerif", Font.BOLD, 15));

        checkoutPanel.add(saleTypeRow);
        checkoutPanel.add(Box.createVerticalStrut(8));
        checkoutPanel.add(customerRow);
        checkoutPanel.add(Box.createVerticalStrut(8));
        checkoutPanel.add(paymentRow);
        checkoutPanel.add(Box.createVerticalStrut(12));
        checkoutPanel.add(totalLabel);
        checkoutPanel.add(Box.createVerticalStrut(10));
        checkoutPanel.add(confirmSaleBtn);

        return checkoutPanel;
    }

    /** Small helper: left-aligned opaque-false flow row for the checkout section. */
    private JPanel makeRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);
        return row;
    }

    // ── Wiring ────────────────────────────────────────────────────────────────

    private void wireActions() {
        addToCartBtn.addActionListener(e -> addSelectedProductToCart());
        removeItemBtn.addActionListener(e -> removeSelectedCartItem());
        clearCartBtn.addActionListener(e -> clearCart());
        confirmSaleBtn.addActionListener(e -> confirmSale());

        // Double-click product row → add to cart
        productsTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) addSelectedProductToCart();
            }
        });

        filterCombo.addActionListener(e -> loadCatalogue());
        sortCombo.addActionListener(e -> loadCatalogue());

        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { loadCatalogue(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { loadCatalogue(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { loadCatalogue(); }
        });

        accountHolderRadio.addActionListener(e -> {
            customerRow.setVisible(true);
            selectedCustomer = null;
            customerSelectedLabel.setText("No customer selected");
            paymentCombo.setModel(new DefaultComboBoxModel<>(new String[]{"CARD", "CREDIT_ACCOUNT"}));
        });

        occasionalCustomerRadio.addActionListener(e -> {
            customerRow.setVisible(false);
            selectedCustomer = null;
            paymentCombo.setModel(new DefaultComboBoxModel<>(new String[]{"CASH", "CARD"}));
        });

        selectCustomerBtn.addActionListener(e -> {
            List<Customer> customers = CustomerDB.getAllActiveCustomers();
            if (customers.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No customers found.");
                return;
            }
            Customer chosen = (Customer) JOptionPane.showInputDialog(
                    this, "Select account holder:", "Customer",
                    JOptionPane.PLAIN_MESSAGE, null,
                    customers.toArray(), customers.get(0));
            if (chosen != null) {
                if (!chosen.isActive()) {
                    JOptionPane.showMessageDialog(this,
                            "The selected account is " + chosen.getAccountStatus() + ".");
                    return;
                }
                selectedCustomer = chosen;
                customerSelectedLabel.setText(chosen.getFullName()
                        + "   |   Balance £" + String.format("%.2f", chosen.getCurrentBalance()));
            }
        });

        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentShown(java.awt.event.ComponentEvent e) {
                loadCatalogue();
            }
        });
    }

    // ── Catalogue ─────────────────────────────────────────────────────────────

    private void loadCatalogue() {
        productsModel.setRowCount(0);
        List<Product> products = ProductDB.getAllProducts();
        String keyword = searchField != null &&
                !searchField.getText().equals("Search products...")
                ? searchField.getText().trim().toLowerCase() : "";

        if (!keyword.isEmpty()) {
            products.removeIf(p ->
                    !p.getDescription().toLowerCase().contains(keyword) &&
                    !p.getItemId().toLowerCase().contains(keyword));
        }

        String filter = (String) filterCombo.getSelectedItem();
        String sort   = (String) sortCombo.getSelectedItem();

        products.removeIf(p ->
                "In Stock".equals(filter)  && p.getStockQuantity() <= 0 ||
                "Low Stock".equals(filter) && !p.isLowStock());

        if ("Sort by: Price".equals(sort)) {
            products.sort((a, b) -> Double.compare(a.getPrice(), b.getPrice()));
        } else if ("Sort by: Stock".equals(sort)) {
            products.sort((a, b) -> Integer.compare(b.getStockQuantity(), a.getStockQuantity()));
        } else {
            products.sort((a, b) -> a.getDescription().compareToIgnoreCase(b.getDescription()));
        }

        for (Product p : products) {
            productsModel.addRow(new Object[]{
                    p.getProductId(),                          // col 0 — hidden
                    p.getDescription(),                        // col 1
                    String.format("%.2f", p.getPrice()),       // col 2
                    p.getStockQuantity()                       // col 3
            });
        }
    }

    // ── Cart operations ───────────────────────────────────────────────────────

    private void addSelectedProductToCart() {
        int row = productsTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a product first.");
            return;
        }

        int productId = (int) productsModel.getValueAt(row, 0);
        Product product = ProductDB.getById(productId);
        if (product == null) {
            JOptionPane.showMessageDialog(this, "Could not load the selected product.");
            return;
        }

        int maxQty = product.getStockQuantity();
        if (maxQty <= 0) {
            JOptionPane.showMessageDialog(this, product.getDescription() + " is out of stock.");
            return;
        }

        JSpinner qtySpinner = new JSpinner(new SpinnerNumberModel(1, 1, maxQty, 1));
        qtySpinner.setPreferredSize(new Dimension(90, 32));

        int result = JOptionPane.showConfirmDialog(
                this,
                new Object[]{"Quantity  (max " + maxQty + "):", qtySpinner},
                "Add to Cart",
                JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) return;

        int qty = (int) qtySpinner.getValue();
        cart.merge(product, qty, Integer::sum);
        refreshCart();
    }

    private void removeSelectedCartItem() {
        int row = cartTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select an item in the cart to remove.");
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
            Product p   = entry.getKey();
            int     qty = entry.getValue();
            double lineTotal = p.getPrice() * qty * (1 + p.getVatRate() / 100.0);
            total += lineTotal;

            cartModel.addRow(new Object[]{
                    p.getDescription(),
                    qty,
                    String.format("%.2f", p.getPrice()),
                    String.format("%.2f", lineTotal)
            });
        }

        int n = cart.size();
        currentCartLabel.setText(n == 0 ? "Cart" : "Cart  (" + n + " item" + (n == 1 ? "" : "s") + ")");
        totalLabel.setText("Total: £" + String.format("%.2f", total));
    }

    private void clearCart() {
        cart.clear();
        selectedCustomer = null;
        customerSelectedLabel.setText("No customer selected");
        occasionalCustomerRadio.setSelected(true);
        customerRow.setVisible(false);
        paymentCombo.setModel(new DefaultComboBoxModel<>(new String[]{"CASH", "CARD"}));
        refreshCart();
    }

    // ── Confirm sale ──────────────────────────────────────────────────────────

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

        String paymentMethod = (String) paymentCombo.getSelectedItem();

        // Card payment — collect details and authorise via PU API
        if ("CARD".equals(paymentMethod)) {
            JTextField cardNumberField = new JTextField(16);
            JTextField expiryField     = new JTextField(5);
            JPasswordField cvvField    = new JPasswordField(4);

            JPanel cardPanel = new JPanel(new GridLayout(3, 2, 8, 8));
            cardPanel.add(new JLabel("Card Number:"));   cardPanel.add(cardNumberField);
            cardPanel.add(new JLabel("Expiry (MM/YY):")); cardPanel.add(expiryField);
            cardPanel.add(new JLabel("CVV:"));           cardPanel.add(cvvField);

            int cardChoice = JOptionPane.showConfirmDialog(
                    this, cardPanel, "Card Payment Details", JOptionPane.OK_CANCEL_OPTION);
            if (cardChoice != JOptionPane.OK_OPTION) return;

            String cardNumber = cardNumberField.getText().trim();
            String expiry     = expiryField.getText().trim();
            String cvv        = new String(cvvField.getPassword()).trim();

            if (cardNumber.isEmpty() || expiry.isEmpty() || cvv.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter all card details.");
                return;
            }

            double total = cart.entrySet().stream()
                    .mapToDouble(e -> e.getKey().getPrice() * e.getValue()
                            * (1 + e.getKey().getVatRate() / 100.0))
                    .sum();

            String result = PUApiClient.processCardPayment(cardNumber, expiry, cvv, total);
            if ("declined".equals(result)) {
                JOptionPane.showMessageDialog(this,
                        "Card payment declined. Please try another payment method.",
                        "Payment Declined", JOptionPane.WARNING_MESSAGE);
                return;
            } else if ("error".equals(result)) {
                JOptionPane.showMessageDialog(this,
                        "Could not reach the payment gateway. Please try again or use a different method.",
                        "Payment Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        // Discount calculation
        double discountAmount = 0.00;
        if (accountSale && selectedCustomer != null && selectedCustomer.getDiscountPlanId() > 0) {
            domain.DiscountPlan plan = database.DiscountPlanDB.getById(selectedCustomer.getDiscountPlanId());
            if (plan != null) {
                double subtotalForDiscount = cart.entrySet().stream()
                        .mapToDouble(e -> e.getKey().getPrice() * e.getValue()).sum();
                if (plan.isFixed()) {
                    discountAmount = subtotalForDiscount * (plan.getDiscountPercent() / 100.0);
                } else if (plan.isFlexible()) {
                    double pendingCredit = database.DiscountPlanDB.getPendingCredit(selectedCustomer.getCustomerId());
                    discountAmount = Math.min(pendingCredit, subtotalForDiscount);
                }
            }
        }

        String saleType = accountSale ? "ACCOUNT" : "OCCASIONAL";

        int saleId = SaleDB.recordSale(
                Session.getUserId(), selectedCustomer,
                saleType, paymentMethod, cart, discountAmount);

        if (saleId == -1) {
            JOptionPane.showMessageDialog(this, "Sale failed. Please try again.");
            return;
        }

        // Consume flexible credit applied
        if (accountSale && discountAmount > 0 && selectedCustomer != null
                && selectedCustomer.getDiscountPlanId() > 0) {
            domain.DiscountPlan plan = database.DiscountPlanDB.getById(selectedCustomer.getDiscountPlanId());
            if (plan != null && plan.isFlexible()) {
                database.DiscountPlanDB.consumeCredit(selectedCustomer.getCustomerId(), discountAmount);
            }
        }

        if (accountSale) {
            double subtotal   = cart.entrySet().stream()
                    .mapToDouble(e -> e.getKey().getPrice() * e.getValue()).sum();
            double vatAmount  = cart.entrySet().stream()
                    .mapToDouble(e -> e.getKey().getPrice() * e.getValue()
                            * (e.getKey().getVatRate() / 100.0)).sum();
            double amountDue  = subtotal - discountAmount + vatAmount;

            String invoiceNumber = SaleDB.generateInvoice(saleId, selectedCustomer.getCustomerId(), amountDue);

            int printChoice = JOptionPane.showConfirmDialog(this,
                    "Sale recorded.\nInvoice: " + invoiceNumber
                    + "\nCustomer: " + selectedCustomer.getFullName()
                    + "\n\nGenerate invoice PDF?",
                    "Sale Complete", JOptionPane.YES_NO_OPTION);

            if (printChoice == JOptionPane.YES_OPTION) {
                List<PdfGenerator.InvoiceItem> pdfItems = new ArrayList<>();
                for (Map.Entry<Product, Integer> entry : cart.entrySet()) {
                    Product p   = entry.getKey();
                    int     qty = entry.getValue();
                    double lineTotal = p.getPrice() * qty * (1 + p.getVatRate() / 100.0);
                    pdfItems.add(new PdfGenerator.InvoiceItem(
                            p.getDescription(), qty, p.getPrice(), lineTotal, p.getVatRate()));
                }
                PdfGenerator.generateRetailInvoice(
                        this, selectedCustomer, invoiceNumber,
                        pdfItems, subtotal, discountAmount, vatAmount, amountDue,
                        Session.getCurrentUser().getFullName());
            }
        } else {
            String receiptNumber = SaleDB.generateReceipt(saleId);
            JOptionPane.showMessageDialog(this, "Sale recorded.\nReceipt: " + receiptNumber);
        }

        clearCart();
        loadCatalogue();
    }

    // ── Table helpers ─────────────────────────────────────────────────────────

    private void configureTable(JTable table) {
        table.setRowHeight(46);
        table.setShowGrid(true);
        table.setIntercellSpacing(new Dimension(1, 1));
        table.setFillsViewportHeight(true);
        table.setBorder(BorderFactory.createEmptyBorder());
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setDefaultEditor(Object.class, null);
        table.setFont(new Font("SansSerif", Font.PLAIN, 13));

        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setFont(new Font("SansSerif", Font.BOLD, 13));
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 32));
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
        if (sp.getVerticalScrollBar()   != null) sp.getVerticalScrollBar().setBackground(ThemeManager.panelBackground());
        if (sp.getHorizontalScrollBar() != null) sp.getHorizontalScrollBar().setBackground(ThemeManager.panelBackground());
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

    // ── Button / control styles ───────────────────────────────────────────────

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
        button.setFont(new Font("SansSerif", Font.BOLD, 13));
        button.setBackground(ThemeManager.buttonLight());
        button.setForeground(ThemeManager.textPrimary());
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.borderColor()),
                new EmptyBorder(6, 12, 6, 12)));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private void styleRadio(JRadioButton rb) {
        rb.setOpaque(false);
        rb.setForeground(ThemeManager.textPrimary());
        rb.setFont(new Font("SansSerif", Font.PLAIN, 13));
        rb.setFocusPainted(false);
        rb.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    // ── Theme ─────────────────────────────────────────────────────────────────

    @Override
    public void applyTheme() {
        setBackground(ThemeManager.appBackground());

        if (contentPanel   != null) contentPanel.setBackground(ThemeManager.appBackground());
        if (centerSection  != null) centerSection.setBackground(ThemeManager.appBackground());
        if (leftCard       != null) leftCard.setBackground(ThemeManager.panelBackground());
        if (rightCard      != null) rightCard.setBackground(ThemeManager.panelBackground());
        if (checkoutPanel  != null) {
            checkoutPanel.setBackground(ThemeManager.panelBackground());
            checkoutPanel.setBorder(BorderFactory.createCompoundBorder(
                    new MatteBorder(1, 0, 0, 0, ThemeManager.borderColor()),
                    new EmptyBorder(14, 0, 0, 0)
            ));
        }
        if (customerRow != null) customerRow.setBackground(ThemeManager.panelBackground());

        if (availableProductsLabel != null) availableProductsLabel.setForeground(ThemeManager.textPrimary());
        if (currentCartLabel       != null) currentCartLabel.setForeground(ThemeManager.textPrimary());
        if (customerSelectedLabel  != null) customerSelectedLabel.setForeground(ThemeManager.textSecondary());
        if (totalLabel             != null) totalLabel.setForeground(ThemeManager.textPrimary());

        if (productsTable != null) applyTableTheme(productsTable);
        if (cartTable     != null) applyTableTheme(cartTable);
        if (productsScrollPane != null) styleScrollPane(productsScrollPane);
        if (cartScrollPane     != null) styleScrollPane(cartScrollPane);

        if (addToCartBtn   != null) stylePrimaryButton(addToCartBtn);
        if (confirmSaleBtn != null) stylePrimaryButton(confirmSaleBtn);
        if (removeItemBtn  != null) styleSecondaryButton(removeItemBtn);
        if (clearCartBtn   != null) styleSecondaryButton(clearCartBtn);
        if (selectCustomerBtn != null) styleSecondaryButton(selectCustomerBtn);

        if (filterCombo    != null) ThemeManager.styleComboBox(filterCombo);
        if (sortCombo      != null) ThemeManager.styleComboBox(sortCombo);
        if (paymentCombo   != null) ThemeManager.styleComboBox(paymentCombo);

        if (searchField != null) {
            ThemeManager.styleTextField(searchField);
            boolean placeholder = "Search products...".equals(searchField.getText());
            searchField.setForeground(placeholder ? ThemeManager.textSecondary() : ThemeManager.fieldForeground());
        }

        if (accountHolderRadio      != null) styleRadio(accountHolderRadio);
        if (occasionalCustomerRadio != null) styleRadio(occasionalCustomerRadio);

        repaint();
        revalidate();
    }
}
