
package ui;

import database.CustomerDB;
import database.DiscountPlanDB;
import domain.Customer;
import domain.DiscountPlan;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.List;

public class CustomerPanel extends JPanel implements ThemeManager.ThemeListener {

    private final ScreenRouter router;

    private JPanel contentPanel;
    private JPanel topControls;
    private JPanel bottomControls;
    private JPanel tableCard;

    private JTable table;
    private JScrollPane scrollPane;
    private DefaultTableModel tableModel;

    private JButton searchCustomerBtn;
    private JButton addCustomerBtn;
    private JButton changeStatusBtn;
    private JButton recordPaymentBtn;
    private JButton refreshBtn;

    private JTextField searchField;
    private JComboBox<String> filterCombo;
    private JComboBox<String> sortCombo;

    public CustomerPanel(ScreenRouter router) {
        this.router = router;

        setLayout(new BorderLayout());
        ThemeManager.register(this);

        AppShell shell = new AppShell(
                router,
                MainFrame.SCREEN_CUSTOMERS,
                "Customer Information",
                "Manage customer accounts",
                buildContent()
        );

        add(shell, BorderLayout.CENTER);
        wireActions();
        loadTable();
        applyTheme();
    }

    private JPanel buildContent() {
        contentPanel = new JPanel(new BorderLayout(20, 20));
        contentPanel.setBorder(new EmptyBorder(8, 8, 8, 8));

        topControls = new JPanel(new BorderLayout(20, 0));
        topControls.setOpaque(false);

        JPanel leftControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        leftControls.setOpaque(false);

        searchCustomerBtn = new JButton("Search Customer");
        searchField = new JTextField(18);

        leftControls.add(searchCustomerBtn);
        leftControls.add(searchField);

        JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        rightControls.setOpaque(false);

        filterCombo = new JComboBox<>(new String[]{"All Customers", "ACTIVE", "SUSPENDED", "IN_DEFAULT"});
        sortCombo = new JComboBox<>(new String[]{"Sort by: Name", "Sort by: Balance", "Sort by: Credit Limit"});

        rightControls.add(filterCombo);
        rightControls.add(sortCombo);

        topControls.add(leftControls, BorderLayout.WEST);
        topControls.add(rightControls, BorderLayout.EAST);

        tableCard = AppShell.createCard();
        tableCard.setLayout(new BorderLayout());

        tableModel = new DefaultTableModel(
                new String[]{"Customer ID", "Name", "Discount Plan", "Credit Limit", "Monthly", "Status"}, 0
        ) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };

        table = new JTable(tableModel);

        configureTable(table);

        scrollPane = new JScrollPane(table);
        styleScrollPane(scrollPane);

        tableCard.add(scrollPane, BorderLayout.CENTER);

        bottomControls = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        bottomControls.setOpaque(false);

        addCustomerBtn = new JButton("Add Customer");
        changeStatusBtn = new JButton("Change Status");
        recordPaymentBtn = new JButton("Record Payment");
        refreshBtn = new JButton("Refresh");

        bottomControls.add(addCustomerBtn);
        bottomControls.add(changeStatusBtn);
        bottomControls.add(recordPaymentBtn);
        bottomControls.add(refreshBtn);

        contentPanel.add(topControls, BorderLayout.NORTH);
        contentPanel.add(tableCard, BorderLayout.CENTER);
        contentPanel.add(bottomControls, BorderLayout.SOUTH);

        return contentPanel;
    }

    private void wireActions() {
        Runnable refreshAction = this::loadTable;

        refreshBtn.addActionListener(e -> refreshAction.run());
        searchCustomerBtn.addActionListener(e -> refreshAction.run());
        searchField.addActionListener(e -> refreshAction.run());
        filterCombo.addActionListener(e -> refreshAction.run());
        sortCombo.addActionListener(e -> refreshAction.run());

        addCustomerBtn.addActionListener(e -> showAddDialog());

        changeStatusBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Please select a customer.");
                return;
            }
            showChangeStatusDialog(row);
        });

        recordPaymentBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Please select a customer.");
                return;
            }
            showRecordPaymentDialog(row);
        });

        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                loadTable();
            }
        });
    }

    private void loadTable() {
        tableModel.setRowCount(0);

        String keyword = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String statusFilter = (String) filterCombo.getSelectedItem();
        String sort = (String) sortCombo.getSelectedItem();

        List<Customer> customers = CustomerDB.getAllActiveCustomers();
        customers.removeIf(c ->
                (!keyword.isEmpty() &&
                        !c.getFullName().toLowerCase().contains(keyword) &&
                        !c.getAccountNumber().toLowerCase().contains(keyword)) ||
                (!"All Customers".equals(statusFilter) && !c.getAccountStatus().equals(statusFilter))
        );

        if ("Sort by: Balance".equals(sort)) {
            customers.sort((a, b) -> Double.compare(b.getCurrentBalance(), a.getCurrentBalance()));
        } else if ("Sort by: Credit Limit".equals(sort)) {
            customers.sort((a, b) -> Double.compare(b.getCreditLimit(), a.getCreditLimit()));
        } else {
            customers.sort((a, b) -> a.getFullName().compareToIgnoreCase(b.getFullName()));
        }

        for (Customer c : customers) {
            String discountText = "None";
            if (c.getDiscountPlanId() > 0) {
                DiscountPlan plan = DiscountPlanDB.getById(c.getDiscountPlanId());
                discountText = plan != null ? plan.getPlanName() : ("Plan #" + c.getDiscountPlanId());
            }

            tableModel.addRow(new Object[]{
                    c.getCustomerId(),
                    c.getFullName() + " (" + c.getAccountNumber() + ")",
                    discountText,
                    String.format("£%.2f", c.getCreditLimit()),
                    String.format("£%.2f", c.getCurrentBalance()),
                    c.getAccountStatus()
            });
        }
    }

    private void showAddDialog() {
        JTextField accNumField = new JTextField();
        JTextField nameField = new JTextField();
        JTextField emailField = new JTextField();
        JTextField phoneField = new JTextField();
        JTextField addressField = new JTextField();
        JTextField limitField = new JTextField("0.00");

        List<DiscountPlan> plans = DiscountPlanDB.getAllPlans();
        DiscountPlan[] planArr = plans.toArray(new DiscountPlan[0]);
        JComboBox<DiscountPlan> planCombo = new JComboBox<>(planArr);
        planCombo.insertItemAt(null, 0);
        planCombo.setSelectedIndex(0);

        JButton newPlanBtn = new JButton("+ New Plan");
        JPanel planPanel = new JPanel(new BorderLayout(4, 0));
        planPanel.add(planCombo, BorderLayout.CENTER);
        planPanel.add(newPlanBtn, BorderLayout.EAST);

        newPlanBtn.addActionListener(e -> {
            showAddDiscountPlanDialog();
            List<DiscountPlan> updated = DiscountPlanDB.getAllPlans();
            planCombo.removeAllItems();
            planCombo.addItem(null);
            for (DiscountPlan p : updated) planCombo.addItem(p);
        });

        Object[] fields = {
                "Account Number:", accNumField,
                "Full Name:", nameField,
                "Email:", emailField,
                "Phone:", phoneField,
                "Address:", addressField,
                "Credit Limit £:", limitField,
                "Discount Plan:", planPanel
        };

        int result = JOptionPane.showConfirmDialog(this, fields, "Add Customer", JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) return;

        if (accNumField.getText().trim().isEmpty() || nameField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Account number and name are required.");
            return;
        }

        try {
            DiscountPlan selectedPlan = (DiscountPlan) planCombo.getSelectedItem();
            int planId = selectedPlan != null ? selectedPlan.getDiscountPlanId() : 0;

            Customer customer = new Customer(
                    0,
                    accNumField.getText().trim(),
                    nameField.getText().trim(),
                    emailField.getText().trim(),
                    phoneField.getText().trim(),
                    addressField.getText().trim(),
                    Double.parseDouble(limitField.getText().trim()),
                    0.00,
                    "ACTIVE",
                    planId
            );

            if (CustomerDB.addCustomer(customer)) {
                loadTable();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to add customer. Account number may already exist.");
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid credit limit.");
        }
    }

    private void showChangeStatusDialog(int row) {
        int customerId = (int) tableModel.getValueAt(row, 0);
        Customer customer = CustomerDB.getById(customerId);
        if (customer == null) {
            JOptionPane.showMessageDialog(this, "Could not load the selected customer.");
            return;
        }

        String[] options = {"ACTIVE", "SUSPENDED", "IN_DEFAULT"};
        String chosen = (String) JOptionPane.showInputDialog(
                this,
                "Change status for " + customer.getFullName(),
                "Change Status",
                JOptionPane.PLAIN_MESSAGE,
                null,
                options,
                customer.getAccountStatus()
        );

        if (chosen != null && !chosen.equals(customer.getAccountStatus())) {
            CustomerDB.updateStatus(customerId, chosen);
            loadTable();
        }
    }

    private void showRecordPaymentDialog(int row) {
        int customerId = (int) tableModel.getValueAt(row, 0);
        Customer customer = CustomerDB.getById(customerId);
        if (customer == null) {
            JOptionPane.showMessageDialog(this, "Could not load the selected customer.");
            return;
        }

        JTextField amountField = new JTextField();
        JComboBox<String> methodCombo = new JComboBox<>(new String[]{"CARD", "BANK_TRANSFER", "CASH"});

        Object[] fields = {
                "Customer: " + customer.getFullName(),
                "Current Balance: £" + String.format("%.2f", customer.getCurrentBalance()),
                "Payment Amount £:", amountField,
                "Payment Method:", methodCombo
        };

        int result = JOptionPane.showConfirmDialog(this, fields, "Record Payment", JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) return;

        try {
            double amount = Double.parseDouble(amountField.getText().trim());
            if (amount <= 0) throw new NumberFormatException();

            double newBalance = Math.max(0, customer.getCurrentBalance() - amount);
            CustomerDB.updateBalance(customerId, newBalance);

            if ("SUSPENDED".equals(customer.getAccountStatus()) && newBalance == 0) {
                CustomerDB.updateStatus(customerId, "ACTIVE");
            }

            CustomerDB.recordAccountPayment(
                    customerId,
                    0,
                    app.Session.getUserId(),
                    (String) methodCombo.getSelectedItem(),
                    amount,
                    "Recorded from customer panel"
            );

            loadTable();
            JOptionPane.showMessageDialog(
                    this,
                    "Payment recorded.\nNew balance: £" + String.format("%.2f", newBalance)
            );
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid payment amount.");
        }
    }

    private void showAddDiscountPlanDialog() {
        JTextField nameField = new JTextField();
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"PERCENTAGE", "ACCOUNT"});
        JTextField discountField = new JTextField("0.00");
        JTextField notesField = new JTextField();

        Object[] fields = {
                "Plan Name:", nameField,
                "Plan Type:", typeCombo,
                "Discount %:", discountField,
                "Notes:", notesField
        };

        int result = JOptionPane.showConfirmDialog(this, fields, "Add Discount Plan", JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) return;

        try {
            DiscountPlan plan = new DiscountPlan(
                    0,
                    nameField.getText().trim(),
                    (String) typeCombo.getSelectedItem(),
                    Double.parseDouble(discountField.getText().trim()),
                    notesField.getText().trim()
            );

            if (!DiscountPlanDB.addPlan(plan)) {
                JOptionPane.showMessageDialog(this, "Could not add the discount plan.");
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid discount percentage.");
        }
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

    private void styleCombo(JComboBox<String> comboBox) {
        comboBox.setBackground(ThemeManager.comboBackground());
        comboBox.setForeground(ThemeManager.comboForeground());
        comboBox.setBorder(BorderFactory.createLineBorder(ThemeManager.borderColor()));
        comboBox.setFocusable(false);
    }

    private void styleField(JTextField field) {
        field.setBackground(ThemeManager.fieldBackground());
        field.setForeground(ThemeManager.fieldForeground());
        field.setCaretColor(ThemeManager.fieldForeground());
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.borderColor()),
                new EmptyBorder(8, 10, 8, 10)
        ));
    }

    @Override
    public void applyTheme() {
        setBackground(ThemeManager.appBackground());

        if (contentPanel != null) contentPanel.setBackground(ThemeManager.appBackground());
        if (topControls != null) topControls.setBackground(ThemeManager.appBackground());
        if (bottomControls != null) bottomControls.setBackground(ThemeManager.appBackground());
        if (tableCard != null) tableCard.setBackground(ThemeManager.panelBackground());

        if (table != null) applyTableTheme(table);
        if (scrollPane != null) styleScrollPane(scrollPane);

        if (searchCustomerBtn != null) stylePrimaryButton(searchCustomerBtn);
        if (addCustomerBtn != null) styleSecondaryButton(addCustomerBtn);
        if (changeStatusBtn != null) styleSecondaryButton(changeStatusBtn);
        if (recordPaymentBtn != null) styleSecondaryButton(recordPaymentBtn);
        if (refreshBtn != null) styleSecondaryButton(refreshBtn);

        if (searchField != null) styleField(searchField);
        if (filterCombo != null) styleCombo(filterCombo);
        if (sortCombo != null) styleCombo(sortCombo);

        repaint();
        revalidate();
    }
}
