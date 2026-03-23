package ui;


import app.Session;
import database.CustomerDB;
import database.ProductDB;
import database.SaleDB;
import domain.Customer;
import domain.Product;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SalesPanel extends JPanel {

    private DefaultTableModel cartModel;
    private Map<Product, Integer> cart = new LinkedHashMap<>();

    private JLabel customerLabel;
    private JLabel totalLabel;
    private Customer selectedCustomer = null;

    private JRadioButton accountBtn;
    private JRadioButton occasionalBtn;
    private JComboBox<String> paymentCombo;

    public SalesPanel(ScreenRouter router) {
        setLayout(new BorderLayout());

        // --- HEADER ---
        JPanel header = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Record Sale", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 28));
        JButton backBtn = new JButton("Back");
        backBtn.addActionListener(e -> router.goTo(MainFrame.SCREEN_DASHBOARD));
        header.add(backBtn, BorderLayout.WEST);
        header.add(title, BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        // --- MAIN AREA ---
        JPanel main = new JPanel(new BorderLayout(10, 10));
        main.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // LEFT — product catalogue
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createTitledBorder("Catalogue"));

        String[] prodCols = {"Item ID", "Description", "Price £", "VAT %", "Stock"};
        DefaultTableModel prodModel = new DefaultTableModel(prodCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable prodTable = new JTable(prodModel);
        prodTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JButton addToCartBtn = new JButton("Add to Cart");

        leftPanel.add(new JScrollPane(prodTable), BorderLayout.CENTER);
        leftPanel.add(addToCartBtn, BorderLayout.SOUTH);

        // RIGHT — cart
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createTitledBorder("Cart"));

        String[] cartCols = {"Description", "Qty", "Unit £", "Line Total £"};
        cartModel = new DefaultTableModel(cartCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable cartTable = new JTable(cartModel);

        totalLabel = new JLabel("Total: £0.00", SwingConstants.RIGHT);
        totalLabel.setFont(new Font("SansSerif", Font.BOLD, 14));

        JButton removeBtn = new JButton("Remove Item");
        JPanel cartBottom = new JPanel(new BorderLayout());
        cartBottom.add(removeBtn, BorderLayout.WEST);
        cartBottom.add(totalLabel, BorderLayout.EAST);

        rightPanel.add(new JScrollPane(cartTable), BorderLayout.CENTER);
        rightPanel.add(cartBottom, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        split.setDividerLocation(500);
        main.add(split, BorderLayout.CENTER);

        // BOTTOM — sale info
        JPanel saleInfo = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 8));

        accountBtn    = new JRadioButton("Account Holder");
        occasionalBtn = new JRadioButton("Occasional Customer", true);
        ButtonGroup typeGroup = new ButtonGroup();
        typeGroup.add(accountBtn);
        typeGroup.add(occasionalBtn);

        JButton selectCustomerBtn = new JButton("Select Customer");
        customerLabel = new JLabel("No customer selected");
        customerLabel.setForeground(Color.GRAY);

        paymentCombo = new JComboBox<>(new String[]{"CASH", "CARD"});

        JButton confirmBtn = new JButton("Confirm Sale");
        confirmBtn.setFont(new Font("SansSerif", Font.BOLD, 13));

        saleInfo.add(new JLabel("Sale type:"));
        saleInfo.add(accountBtn);
        saleInfo.add(occasionalBtn);
        saleInfo.add(selectCustomerBtn);
        saleInfo.add(customerLabel);
        saleInfo.add(new JLabel("Payment:"));
        saleInfo.add(paymentCombo);
        saleInfo.add(confirmBtn);

        main.add(saleInfo, BorderLayout.SOUTH);
        add(main, BorderLayout.CENTER);

        // --- ACTIONS ---

        // switch sale type → update payment options
        accountBtn.addActionListener(e -> {
            paymentCombo.setModel(new DefaultComboBoxModel<>(new String[]{"CARD", "CREDIT_ACCOUNT"}));
            selectCustomerBtn.setEnabled(true);
        });
        occasionalBtn.addActionListener(e -> {
            paymentCombo.setModel(new DefaultComboBoxModel<>(new String[]{"CASH", "CARD"}));
            selectedCustomer = null;
            customerLabel.setText("No customer selected");
            selectCustomerBtn.setEnabled(false);
        });
        selectCustomerBtn.setEnabled(false);

        selectCustomerBtn.addActionListener(e -> {
            List<Customer> customers = CustomerDB.getAllActiveCustomers();
            if (customers.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No customers found.");
                return;
            }
            Customer[] arr = customers.toArray(new Customer[0]);
            Customer chosen = (Customer) JOptionPane.showInputDialog(
                    this, "Select account holder:", "Customer",
                    JOptionPane.PLAIN_MESSAGE, null, arr, arr[0]);
            if (chosen != null) {
                if (!chosen.isActive()) {
                    JOptionPane.showMessageDialog(this,
                            "Account is " + chosen.getAccountStatus() + ". Cannot process sale.");
                    return;
                }
                selectedCustomer = chosen;
                customerLabel.setText(chosen.getFullName() + " | Balance: £"
                        + String.format("%.2f", chosen.getCurrentBalance())
                        + " / Limit: £" + String.format("%.2f", chosen.getCreditLimit()));
            }
        });

        // load catalogue on show
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                loadCatalogue(prodModel);
                clearCart();
            }
        });

        addToCartBtn.addActionListener(e -> {
            int row = prodTable.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Please select a product.");
                return;
            }

            int productId = (int) prodModel.getValueAt(row, 0);
            Product product = ProductDB.getAllProducts().stream()
                    .filter(p -> p.getProductId() == productId)
                    .findFirst().orElse(null);

            if (product == null) return;

            String input = JOptionPane.showInputDialog(this, "Quantity:");
            if (input == null || input.trim().isEmpty()) return;

            try {
                int qty = Integer.parseInt(input.trim());
                if (qty <= 0) throw new NumberFormatException();
                if (qty > product.getStockQuantity()) {
                    JOptionPane.showMessageDialog(this, "Not enough stock. Available: "
                            + product.getStockQuantity());
                    return;
                }
                cart.merge(product, qty, Integer::sum);
                refreshCart();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter a valid quantity.");
            }
        });

        removeBtn.addActionListener(e -> {
            int row = cartTable.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Please select an item to remove.");
                return;
            }
            Product key = (Product) cart.keySet().toArray()[row];
            cart.remove(key);
            refreshCart();
        });

        confirmBtn.addActionListener(e -> confirmSale());
    }

    private void loadCatalogue(DefaultTableModel model) {
        model.setRowCount(0);
        for (Product p : ProductDB.getAllProducts()) {
            model.addRow(new Object[]{
                    p.getProductId(),
                    p.getDescription(),
                    String.format("%.2f", p.getPrice()),
                    String.format("%.2f", p.getVatRate()),
                    p.getStockQuantity()
            });
        }
    }

    private void refreshCart() {
        cartModel.setRowCount(0);
        double total = 0;
        for (Map.Entry<Product, Integer> entry : cart.entrySet()) {
            Product p   = entry.getKey();
            int     qty = entry.getValue();
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
        cartModel.setRowCount(0);
        totalLabel.setText("Total: £0.00");
        selectedCustomer = null;
        customerLabel.setText("No customer selected");
        occasionalBtn.setSelected(true);
        paymentCombo.setModel(new DefaultComboBoxModel<>(new String[]{"CASH", "CARD"}));
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

        String saleType      = isAccount ? "ACCOUNT" : "OCCASIONAL";
        String paymentMethod = (String) paymentCombo.getSelectedItem();
        int    userId        = Session.getCurrentUser().getUserId();

        int saleId = SaleDB.recordSale(userId, selectedCustomer, saleType, paymentMethod, cart, 0.00);

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
            docNumber = SaleDB.generateInvoice(saleId, selectedCustomer.getCustomerId(), total);
            JOptionPane.showMessageDialog(this,
                    "Sale recorded.\nInvoice: " + docNumber
                            + "\nCustomer: " + selectedCustomer.getFullName());
        } else {
            docNumber = SaleDB.generateReceipt(saleId);
            JOptionPane.showMessageDialog(this,
                    "Sale recorded.\nReceipt: " + docNumber);
        }

        clearCart();
    }
}