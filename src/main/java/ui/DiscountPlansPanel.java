package ui;

import database.DiscountPlanDB;
import domain.DiscountPlan;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.List;

public class DiscountPlansPanel extends JPanel implements ThemeManager.ThemeListener {

    private final ScreenRouter router;

    private JPanel contentPanel;
    private JPanel tableCard;
    private JPanel bottomControls;

    private JTable table;
    private JScrollPane scrollPane;
    private DefaultTableModel tableModel;

    private JButton addBtn;
    private JButton editBtn;
    private JButton deleteBtn;
    private JButton refreshBtn;

    public DiscountPlansPanel(ScreenRouter router) {
        this.router = router;

        setLayout(new BorderLayout());
        ThemeManager.register(this);

        AppShell shell = new AppShell(
                router,
                MainFrame.SCREEN_DISCOUNT_PLANS,
                "Discount Plans",
                "Manage fixed and flexible discount plans",
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

        tableCard = AppShell.createCard();
        tableCard.setLayout(new BorderLayout());
        tableCard.setBorder(new EmptyBorder(18, 18, 18, 18));

        tableModel = new DefaultTableModel(
                new String[]{"ID", "Plan Name", "Type", "Discount %", "Notes"}, 0
        ) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        table = new JTable(tableModel);
        configureTable(table);

        scrollPane = new JScrollPane(table);
        styleScrollPane(scrollPane);
        tableCard.add(scrollPane, BorderLayout.CENTER);

        bottomControls = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        bottomControls.setOpaque(false);

        addBtn     = createBtn("Add Plan", true);
        editBtn    = createBtn("Edit Plan", false);
        deleteBtn  = createBtn("Delete Plan", false);
        refreshBtn = createBtn("Refresh", false);

        bottomControls.add(addBtn);
        bottomControls.add(editBtn);
        bottomControls.add(deleteBtn);
        bottomControls.add(refreshBtn);

        contentPanel.add(tableCard, BorderLayout.CENTER);
        contentPanel.add(bottomControls, BorderLayout.SOUTH);

        return contentPanel;
    }

    private void wireActions() {
        refreshBtn.addActionListener(e -> loadTable());

        addBtn.addActionListener(e -> showAddDialog());

        editBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(this, "Please select a plan to edit."); return; }
            showEditDialog(row);
        });

        deleteBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(this, "Please select a plan to delete."); return; }
            int planId   = (int) tableModel.getValueAt(row, 0);
            String name  = (String) tableModel.getValueAt(row, 1);
            int confirm  = JOptionPane.showConfirmDialog(this,
                    "Delete discount plan: " + name + "?\nCustomers using this plan will have their plan removed.",
                    "Confirm Delete", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                DiscountPlanDB.deletePlan(planId);
                loadTable();
            }
        });

        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentShown(java.awt.event.ComponentEvent e) { loadTable(); }
        });
    }

    private void loadTable() {
        tableModel.setRowCount(0);
        for (DiscountPlan p : DiscountPlanDB.getAllPlans()) {
            tableModel.addRow(new Object[]{
                    p.getDiscountPlanId(),
                    p.getPlanName(),
                    p.getPlanType(),
                    String.format("%.2f%%", p.getDiscountPercent()),
                    p.getNotes() != null ? p.getNotes() : ""
            });
        }
    }

    private void showAddDialog() {
        JTextField nameField    = new JTextField();
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"FIXED", "FLEXIBLE"});
        JTextField percentField = new JTextField("0.00");
        JTextField notesField   = new JTextField();

        Object[] fields = {
                "Plan Name:",        nameField,
                "Plan Type:",        typeCombo,
                "Discount %:",       percentField,
                "Notes (optional):", notesField
        };

        int result = JOptionPane.showConfirmDialog(this, fields, "Add Discount Plan", JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) return;

        if (nameField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Plan name is required.");
            return;
        }

        try {
            DiscountPlan plan = new DiscountPlan(
                    nameField.getText().trim(),
                    (String) typeCombo.getSelectedItem(),
                    Double.parseDouble(percentField.getText().trim()),
                    notesField.getText().trim()
            );
            if (DiscountPlanDB.addPlan(plan)) loadTable();
            else JOptionPane.showMessageDialog(this, "Failed to add plan.");
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid discount percentage.");
        }
    }

    private void showEditDialog(int row) {
        int    planId  = (int) tableModel.getValueAt(row, 0);
        DiscountPlan existing = DiscountPlanDB.getById(planId);
        if (existing == null) return;

        JTextField nameField    = new JTextField(existing.getPlanName());
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"FIXED", "FLEXIBLE"});
        typeCombo.setSelectedItem(existing.getPlanType());
        JTextField percentField = new JTextField(String.valueOf(existing.getDiscountPercent()));
        JTextField notesField   = new JTextField(existing.getNotes() != null ? existing.getNotes() : "");

        Object[] fields = {
                "Plan Name:",        nameField,
                "Plan Type:",        typeCombo,
                "Discount %:",       percentField,
                "Notes (optional):", notesField
        };

        int result = JOptionPane.showConfirmDialog(this, fields, "Edit Discount Plan", JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) return;

        try {
            DiscountPlan updated = new DiscountPlan(
                    planId,
                    nameField.getText().trim(),
                    (String) typeCombo.getSelectedItem(),
                    Double.parseDouble(percentField.getText().trim()),
                    notesField.getText().trim()
            );
            DiscountPlanDB.updatePlan(updated);
            loadTable();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid discount percentage.");
        }
    }

    private void configureTable(JTable t) {
        t.setRowHeight(44);
        t.setShowGrid(true);
        t.setIntercellSpacing(new Dimension(1, 1));
        t.setFillsViewportHeight(true);
        t.setBorder(BorderFactory.createEmptyBorder());
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setDefaultEditor(Object.class, null);
        JTableHeader h = t.getTableHeader();
        h.setReorderingAllowed(false);
        h.setFont(new Font("SansSerif", Font.BOLD, 13));
        h.setBorder(BorderFactory.createEmptyBorder());
        DefaultTableCellRenderer r = new DefaultTableCellRenderer();
        r.setHorizontalAlignment(SwingConstants.LEFT);
        r.setBorder(new EmptyBorder(0, 10, 0, 10));
        t.setDefaultRenderer(Object.class, r);
    }

    private void styleScrollPane(JScrollPane sp) {
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setOpaque(true);
        sp.setBackground(ThemeManager.tableBackground());
        sp.getViewport().setBackground(ThemeManager.tableBackground());
        sp.getViewport().setBorder(null);
    }

    private void applyTableTheme() {
        if (table == null) return;
        table.setBackground(ThemeManager.tableBackground());
        table.setForeground(ThemeManager.textPrimary());
        table.setGridColor(ThemeManager.tableGrid());
        table.setSelectionBackground(ThemeManager.selectionBackground());
        table.setSelectionForeground(ThemeManager.textPrimary());
        JTableHeader h = table.getTableHeader();
        if (h != null) {
            h.setBackground(ThemeManager.tableHeaderBackground());
            h.setForeground(ThemeManager.textPrimary());
            h.setOpaque(true);
        }
        DefaultTableCellRenderer r = new DefaultTableCellRenderer();
        r.setBackground(ThemeManager.tableBackground());
        r.setForeground(ThemeManager.textPrimary());
        r.setHorizontalAlignment(SwingConstants.LEFT);
        r.setBorder(new EmptyBorder(0, 10, 0, 10));
        table.setDefaultRenderer(Object.class, r);
    }

    private JButton createBtn(String text, boolean primary) {
        JButton btn = new JButton(text);
        btn.setFocusPainted(false);
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (primary) {
            btn.setBackground(ThemeManager.buttonDark());
            btn.setForeground(ThemeManager.textLight());
            btn.setBorder(new EmptyBorder(10, 18, 10, 18));
        } else {
            btn.setBackground(ThemeManager.buttonLight());
            btn.setForeground(ThemeManager.textPrimary());
            btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ThemeManager.borderColor()),
                    new EmptyBorder(10, 18, 10, 18)
            ));
        }
        return btn;
    }

    @Override
    public void applyTheme() {
        setBackground(ThemeManager.appBackground());
        if (contentPanel   != null) contentPanel.setBackground(ThemeManager.appBackground());
        if (bottomControls != null) bottomControls.setBackground(ThemeManager.appBackground());
        if (tableCard      != null) tableCard.setBackground(ThemeManager.panelBackground());
        if (scrollPane     != null) styleScrollPane(scrollPane);
        applyTableTheme();
        if (addBtn    != null) { addBtn.setBackground(ThemeManager.buttonDark()); addBtn.setForeground(ThemeManager.textLight()); }
        if (editBtn   != null) { editBtn.setBackground(ThemeManager.buttonLight()); editBtn.setForeground(ThemeManager.textPrimary()); }
        if (deleteBtn != null) { deleteBtn.setBackground(ThemeManager.buttonLight()); deleteBtn.setForeground(ThemeManager.textPrimary()); }
        if (refreshBtn!= null) { refreshBtn.setBackground(ThemeManager.buttonLight()); refreshBtn.setForeground(ThemeManager.textPrimary()); }
        repaint(); revalidate();
    }
}