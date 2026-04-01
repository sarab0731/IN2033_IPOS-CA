package ui;

import database.CustomerDB;
import database.DiscountPlanDB;
import domain.Customer;
import domain.DiscountPlan;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CustomerPanel extends JPanel {

    private DefaultTableModel tableModel;
    private JTable table;

    private JTextField searchField;
    private JComboBox<String> filterCombo;
    private JComboBox<String> sortCombo;

    private List<Customer> currentCustomers = new ArrayList<>();

    public CustomerPanel(ScreenRouter router) {
        setLayout(new BorderLayout());
        setBackground(new Color(242, 242, 242));

        JPanel customerContent = buildCustomerContent(router);

        AppShell shell = new AppShell(
                router,
                MainFrame.SCREEN_CUSTOMERS,
                "Product Information",
                "Manage customer accounts",
                customerContent
        );

        add(shell, BorderLayout.CENTER);

        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                loadTable();
            }
        });
    }

    private JPanel buildCustomerContent(ScreenRouter router) {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setOpaque(false);

        JPanel card = AppShell.createCard();
        card.setLayout(new BorderLayout(0, 18));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(232, 232, 232)),
                new EmptyBorder(20, 20, 20, 20)
        ));

        card.add(buildTopBar(), BorderLayout.NORTH);
        card.add(buildTableSection(), BorderLayout.CENTER);
        card.add(buildFooterSection(), BorderLayout.SOUTH);

        outer.add(card, BorderLayout.CENTER);
        return outer;
    }

    private JPanel buildTopBar() {
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);

        JButton searchBtn = buildDarkButton("Search Customer");
        searchBtn.addActionListener(e -> applyFiltersAndSorting());

        searchField = new JTextField(16);
        searchField.setPreferredSize(new Dimension(170, 32));
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)),
                new EmptyBorder(6, 10, 6, 10)
        ));
        searchField.setFont(new Font("SansSerif", Font.PLAIN, 12));

        left.add(searchBtn);
        left.add(searchField);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);

        filterCombo = new JComboBox<>(new String[]{
                "All Customers", "Active", "Suspended", "In Default"
        });

        sortCombo = new JComboBox<>(new String[]{
                "Sort by: Name", "Sort by: Account Number", "Sort by: Credit Limit", "Sort by: Monthly"
        });

        styleCombo(filterCombo);
        styleCombo(sortCombo);

        filterCombo.addActionListener(e -> applyFiltersAndSorting());
        sortCombo.addActionListener(e -> applyFiltersAndSorting());

        right.add(filterCombo);
        right.add(sortCombo);

        top.add(left, BorderLayout.WEST);
        top.add(right, BorderLayout.EAST);

        return top;
    }

    private JPanel buildTableSection() {
        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);

        String[] columns = {
                "Customer ID", "Name", "Discount Plan", "Credit Limit", "Monthly", "Status"
        };

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(42);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFont(new Font("SansSerif", Font.PLAIN, 12));
        table.setForeground(new Color(35, 35, 35));
        table.setSelectionBackground(new Color(235, 235, 235));
        table.setSelectionForeground(new Color(35, 35, 35));

        styleTableHeader(table);

        table.getColumnModel().getColumn(5).setCellRenderer(new StatusCellRenderer());

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);

        center.add(scrollPane, BorderLayout.CENTER);

        return center;
    }

    private JPanel buildFooterSection() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        footer.setOpaque(false);

        JButton addBtn = buildLightButton("Add Customer");
        JButton statusBtn = buildLightButton("Change Status");
        JButton paymentBtn = buildLightButton("Record Payment");
        JButton refreshBtn = buildLightButton("Refresh");

        addBtn.addActionListener(e -> showAddDialog());

        statusBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Please select a customer.");
                return;
            }
            showChangeStatusDialog(row);
        });

        paymentBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Please select a customer.");
                return;
            }
            showRecordPaymentDialog(row);
        });

        refreshBtn.addActionListener(e -> loadTable());

        footer.add(addBtn);
        footer.add(statusBtn);
        footer.add(paymentBtn);
        footer.add(refreshBtn);

        return footer;
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

    private void styleTableHeader(JTable table) {
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.getTableHeader().setBackground(Color.WHITE);
        table.getTableHeader().setForeground(new Color(25, 25, 25));
        table.getTableHeader().setBorder(BorderFactory.createEmptyBorder());
        table.getTableHeader().setReorderingAllowed(false);
    }

    private void loadTable() {
        currentCustomers = CustomerDB.getAllActiveCustomers();
        applyFiltersAndSorting();
    }

    private void applyFiltersAndSorting() {
        if (tableModel == null) {
            return;
        }

        tableModel.setRowCount(0);

        List<Customer> displayList = new ArrayList<>(currentCustomers);

        String keyword = searchField != null ? searchField.getText().trim().toLowerCase() : "";
        String filter = filterCombo != null ? (String) filterCombo.getSelectedItem() : "All Customers";
        String sort = sortCombo != null ? (String) sortCombo.getSelectedItem() : "Sort by: Name";

        if (!keyword.isEmpty()) {
            displayList.removeIf(c ->
                    !(safe(c.getAccountNumber()).toLowerCase().contains(keyword)
                            || safe(c.getFullName()).toLowerCase().contains(keyword)
                            || safe(c.getEmail()).toLowerCase().contains(keyword)
                            || safe(c.getPhone()).toLowerCase().contains(keyword))
            );
        }

        if ("Active".equals(filter)) {
            displayList.removeIf(c -> !"ACTIVE".equalsIgnoreCase(c.getAccountStatus()));
        } else if ("Suspended".equals(filter)) {
            displayList.removeIf(c -> !"SUSPENDED".equalsIgnoreCase(c.getAccountStatus()));
        } else if ("In Default".equals(filter)) {
            displayList.removeIf(c -> !"IN_DEFAULT".equalsIgnoreCase(c.getAccountStatus()));
        }

        if ("Sort by: Name".equals(sort)) {
            displayList.sort(Comparator.comparing(Customer::getFullName, String.CASE_INSENSITIVE_ORDER));
        } else if ("Sort by: Account Number".equals(sort)) {
            displayList.sort(Comparator.comparing(Customer::getAccountNumber, String.CASE_INSENSITIVE_ORDER));
        } else if ("Sort by: Credit Limit".equals(sort)) {
            displayList.sort(Comparator.comparingDouble(Customer::getCreditLimit).reversed());
        } else if ("Sort by: Monthly".equals(sort)) {
            displayList.sort(Comparator.comparingDouble(Customer::getCurrentBalance).reversed());
        }

        for (Customer c : displayList) {
            tableModel.addRow(new Object[]{
                    c.getAccountNumber(),
                    c.getFullName(),
                    getDiscountPlanName(c.getDiscountPlanId()),
                    String.format("£%.2f", c.getCreditLimit()),
                    String.format("£%.2f", c.getCurrentBalance()),
                    formatStatus(c.getAccountStatus())
            });
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String getDiscountPlanName(int planId) {
        if (planId <= 0) {
            return "None";
        }
        DiscountPlan plan = DiscountPlanDB.getById(planId);
        return plan != null ? plan.getPlanName() : "Plan " + planId;
    }

    private String formatStatus(String status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case "ACTIVE" -> "Active";
            case "SUSPENDED" -> "Suspended";
            case "IN_DEFAULT" -> "In Default";
            default -> status;
        };
    }

    private Customer getSelectedCustomerFromTableRow(int row) {
        String accountNumber = tableModel.getValueAt(row, 0).toString();
        return currentCustomers.stream()
                .filter(c -> accountNumber.equals(c.getAccountNumber()))
                .findFirst()
                .orElse(null);
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

        JButton newPlanBtn = buildLightButton("+ New Plan");
        JPanel planPanel = new JPanel(new BorderLayout(4, 0));
        planPanel.add(planCombo, BorderLayout.CENTER);
        planPanel.add(newPlanBtn, BorderLayout.EAST);

        newPlanBtn.addActionListener(e -> {
            showAddDiscountPlanDialog();
            List<DiscountPlan> updated = DiscountPlanDB.getAllPlans();
            planCombo.removeAllItems();
            planCombo.addItem(null);
            for (DiscountPlan p : updated) {
                planCombo.addItem(p);
            }
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
        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        if (accNumField.getText().trim().isEmpty() || nameField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Account number and name are required.");
            return;
        }

        try {
            DiscountPlan selectedPlan = (DiscountPlan) planCombo.getSelectedItem();
            int planId = selectedPlan != null ? selectedPlan.getDiscountPlanId() : 0;

            Customer c = new Customer(
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

            if (CustomerDB.addCustomer(c)) {
                loadTable();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to add customer. Account number may already exist.");
            }

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid credit limit.");
        }
    }

    private void showChangeStatusDialog(int row) {
        Customer customer = getSelectedCustomerFromTableRow(row);
        if (customer == null) {
            JOptionPane.showMessageDialog(this, "Could not load selected customer.");
            return;
        }

        int customerId = customer.getCustomerId();
        String current = customer.getAccountStatus();
        String name = customer.getFullName();

        String[] options;
        if ("IN_DEFAULT".equals(current)) {
            options = new String[]{"ACTIVE", "SUSPENDED"};
        } else {
            options = new String[]{"ACTIVE", "SUSPENDED", "IN_DEFAULT"};
        }

        String chosen = (String) JOptionPane.showInputDialog(
                this,
                "Change status for: " + name + "\nCurrent: " + current,
                "Change Status",
                JOptionPane.PLAIN_MESSAGE,
                null,
                options,
                current
        );

        if (chosen != null && !chosen.equals(current)) {
            CustomerDB.updateStatus(customerId, chosen);
            loadTable();
        }
    }

    private void showRecordPaymentDialog(int row) {
        Customer customer = getSelectedCustomerFromTableRow(row);
        if (customer == null) {
            JOptionPane.showMessageDialog(this, "Could not load selected customer.");
            return;
        }

        int customerId = customer.getCustomerId();
        String name = customer.getFullName();
        double balance = customer.getCurrentBalance();

        JTextField amountField = new JTextField();
        JComboBox<String> methodCombo = new JComboBox<>(new String[]{"CARD", "BANK_TRANSFER", "CASH"});

        Object[] fields = {
                "Customer: " + name,
                "Current Balance: £" + String.format("%.2f", balance),
                "Payment Amount £:", amountField,
                "Payment Method:", methodCombo
        };

        int result = JOptionPane.showConfirmDialog(this, fields, "Record Payment", JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            double amount = Double.parseDouble(amountField.getText().trim());
            if (amount <= 0) {
                throw new NumberFormatException();
            }

            double newBalance = Math.max(0, balance - amount);
            CustomerDB.updateBalance(customerId, newBalance);

            if ("SUSPENDED".equals(customer.getAccountStatus()) && newBalance == 0) {
                CustomerDB.updateStatus(customerId, "ACTIVE");
            }

            loadTable();
            JOptionPane.showMessageDialog(
                    this,
                    "Payment of £" + String.format("%.2f", amount)
                            + " recorded.\nNew balance: £"
                            + String.format("%.2f", newBalance)
            );

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid amount.");
        }
    }

    private void showAddDiscountPlanDialog() {
        JTextField nameField = new JTextField();
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"FIXED", "FLEXIBLE"});
        JTextField percentField = new JTextField();
        JTextField notesField = new JTextField();

        Object[] fields = {
                "Plan Name:", nameField,
                "Plan Type:", typeCombo,
                "Discount Percent:", percentField,
                "Notes:", notesField
        };

        int result = JOptionPane.showConfirmDialog(
                this,
                fields,
                "Add Discount Plan",
                JOptionPane.OK_CANCEL_OPTION
        );

        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            DiscountPlan plan = new DiscountPlan(
                    0,
                    nameField.getText().trim(),
                    typeCombo.getSelectedItem().toString(),
                    Double.parseDouble(percentField.getText().trim()),
                    notesField.getText().trim()
            );

            if (!DiscountPlanDB.addPlan(plan)) {
                JOptionPane.showMessageDialog(this, "Failed to add discount plan.");
            }

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid discount percent.");
        }
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

                switch (status) {
                    case "Active" -> label.setBackground(new Color(201, 224, 98));
                    case "Suspended" -> label.setBackground(new Color(240, 205, 90));
                    case "In Default" -> label.setBackground(new Color(234, 105, 90));
                    default -> label.setBackground(Color.WHITE);
                }
            }

            return label;
        }
    }
}