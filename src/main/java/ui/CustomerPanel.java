package ui;

import database.CustomerDB;
import database.DiscountPlanDB;
import domain.Customer;
import domain.DiscountPlan;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class CustomerPanel extends JPanel {

    private DefaultTableModel tableModel;
    private JTable table;

    public CustomerPanel(ScreenRouter router) {
        setLayout(new BorderLayout());

        // --- HEADER ---
        JPanel header = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Customer Accounts", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 28));
        JButton backBtn = new JButton("Back");
        backBtn.addActionListener(e -> router.goTo(MainFrame.SCREEN_DASHBOARD));
        header.add(backBtn, BorderLayout.WEST);
        header.add(title, BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        // --- TABLE ---
        String[] columns = {"ID", "Account No.", "Full Name", "Email", "Phone",
                "Credit Limit £", "Balance £", "Status", "Discount Plan"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        add(new JScrollPane(table), BorderLayout.CENTER);

        // --- BUTTONS ---
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 8));

        JButton addBtn      = new JButton("Add Customer");
        JButton statusBtn   = new JButton("Change Status");
        JButton paymentBtn  = new JButton("Record Payment");
        JButton refreshBtn  = new JButton("Refresh");

        buttons.add(addBtn);
        buttons.add(statusBtn);
        buttons.add(paymentBtn);
        buttons.add(refreshBtn);
        add(buttons, BorderLayout.SOUTH);

        // --- ACTIONS ---
        refreshBtn.addActionListener(e -> loadTable());

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

        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                loadTable();
            }
        });
    }

    private void loadTable() {
        tableModel.setRowCount(0);
        List<Customer> customers = CustomerDB.getAllActiveCustomers();

        for (Customer c : customers) {
            tableModel.addRow(new Object[]{
                    c.getCustomerId(),
                    c.getAccountNumber(),
                    c.getFullName(),
                    c.getEmail(),
                    c.getPhone(),
                    String.format("%.2f", c.getCreditLimit()),
                    String.format("%.2f", c.getCurrentBalance()),
                    c.getAccountStatus(),
                    c.getDiscountPlanId() > 0 ? c.getDiscountPlanId() : "None"
            });
        }
    }

    private void showAddDialog() {
        JTextField accNumField  = new JTextField();
        JTextField nameField    = new JTextField();
        JTextField emailField   = new JTextField();
        JTextField phoneField   = new JTextField();
        JTextField addressField = new JTextField();
        JTextField limitField   = new JTextField("0.00");

        // discount plan selector
        List<DiscountPlan> plans = DiscountPlanDB.getAllPlans();
        DiscountPlan[] planArr = plans.toArray(new DiscountPlan[0]);
        JComboBox<DiscountPlan> planCombo = new JComboBox<>(planArr);
        planCombo.insertItemAt(null, 0);
        planCombo.setSelectedIndex(0);

        // option to add new discount plan inline
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
                "Account Number:",  accNumField,
                "Full Name:",       nameField,
                "Email:",           emailField,
                "Phone:",           phoneField,
                "Address:",         addressField,
                "Credit Limit £:",  limitField,
                "Discount Plan:",   planPanel
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
        int    customerId = (int) tableModel.getValueAt(row, 0);
        String current    = (String) tableModel.getValueAt(row, 7);
        String name       = (String) tableModel.getValueAt(row, 2);

        // as per brief: only MANAGER/ADMIN can restore IN_DEFAULT
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
        int    customerId = (int) tableModel.getValueAt(row, 0);
        String name       = (String) tableModel.getValueAt(row, 2);
        double balance    = Double.parseDouble((String) tableModel.getValueAt(row, 6));

        JTextField amountField = new JTextField();
        JComboBox<String> methodCombo = new JComboBox<>(new String[]{"CARD", "BANK_TRANSFER", "CASH"});

        Object[] fields = {
                "Customer: " + name,
                "Current Balance: £" + String.format("%.2f", balance),
                "Payment Amount £:", amountField,
                "Payment Method:",   methodCombo
        };

        int result = JOptionPane.showConfirmDialog(this, fields, "Record Payment", JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) return;

        try {
            double amount = Double.parseDouble(amountField.getText().trim());
            if (amount <= 0) throw new NumberFormatException();

            double newBalance = Math.max(0, balance - amount);
            CustomerDB.updateBalance(customerId, newBalance);

            // if account was suspended and balance now cleared, restore to ACTIVE
            String status = (String) tableModel.getValueAt(row, 7);
            if ("SUSPENDED".equals(status) && newBalance == 0) {
                CustomerDB.updateStatus(customerId, "ACTIVE");
            }

            loadTable();
            JOptionPane.showMessageDialog(this,
                    "Payment of £" + String.format("%.2f", amount) + " recorded.\nNew balance: £"
                            + String.format("%.2f", newBalance));

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid amount.");
        }
    }

    private void showAddDiscountPlanDialog() {
        JTextField nameField    = new JTextField();
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"FIXED", "FLEXIBLE"});
        JTextField percentField = new JTextField();
        JTextField notesField   = new JTextField();

        Object[] fields = {
                "Plan Name:",        nameField,
                "Plan Type:",        typeCombo,
                "Discount %:",       percentField,
                "Notes (optional):", notesField
        };

        int result = JOptionPane.showConfirmDialog(this, fields, "New Discount Plan", JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) return;

        try {
            DiscountPlan plan = new DiscountPlan(
                    nameField.getText().trim(),
                    (String) typeCombo.getSelectedItem(),
                    Double.parseDouble(percentField.getText().trim()),
                    notesField.getText().trim()
            );
            DiscountPlanDB.addPlan(plan);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid discount percentage.");
        }
    }
}