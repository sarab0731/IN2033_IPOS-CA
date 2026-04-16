package ui;

import database.DiscountPlanDB;
import domain.DiscountPlan;
import domain.DiscountTier;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;

public class DiscountPlansPanel extends JPanel implements ThemeManager.ThemeListener {

    private final ScreenRouter router;

    private JPanel contentPanel;
    private JPanel leftPanel;
    private JPanel rightPanel;
    private JPanel bottomControls;

    // Plans table
    private JTable plansTable;
    private JScrollPane plansScrollPane;
    private DefaultTableModel plansModel;

    // Tiers table (visible only when a FLEXIBLE plan is selected)
    private JTable tiersTable;
    private JScrollPane tiersScrollPane;
    private DefaultTableModel tiersModel;
    private JPanel tiersCard;
    private JLabel tiersTitle;

    // Credits table
    private JTable creditsTable;
    private JScrollPane creditsScrollPane;
    private DefaultTableModel creditsModel;
    private JPanel creditsCard;

    private JButton addBtn;
    private JButton editBtn;
    private JButton deleteBtn;
    private JButton refreshBtn;
    private JButton addTierBtn;
    private JButton deleteTierBtn;
    private JButton monthEndBtn;

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
        loadPlansTable();
        loadCreditsTable();
        applyTheme();
    }

    private JPanel buildContent() {
        contentPanel = new JPanel(new BorderLayout(16, 16));
        contentPanel.setBorder(new EmptyBorder(8, 8, 8, 8));

        // ── Left: plans + tiers ──────────────────────────────────────────────
        leftPanel = new JPanel(new BorderLayout(0, 12));
        leftPanel.setOpaque(false);

        JPanel plansCard = AppShell.createCard();
        plansCard.setLayout(new BorderLayout());
        plansCard.setBorder(new EmptyBorder(12, 12, 12, 12));

        TitledBorder plansBorder = BorderFactory.createTitledBorder("Discount Plans");
        plansBorder.setTitleFont(new Font("SansSerif", Font.BOLD, 13));
        plansCard.setBorder(plansBorder);

        plansModel = new DefaultTableModel(
                new String[]{"ID", "Plan Name", "Type", "Discount %", "Notes"}, 0
        ) { @Override public boolean isCellEditable(int r, int c) { return false; } };

        plansTable = new JTable(plansModel);
        configureTable(plansTable);
        plansScrollPane = new JScrollPane(plansTable);
        styleScrollPane(plansScrollPane);
        plansScrollPane.setPreferredSize(new Dimension(0, 200));
        plansCard.add(plansScrollPane, BorderLayout.CENTER);

        JPanel planBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        planBtns.setOpaque(false);
        addBtn     = createBtn("Add Plan",    true);
        editBtn    = createBtn("Edit Plan",   false);
        deleteBtn  = createBtn("Delete Plan", false);
        refreshBtn = createBtn("Refresh",     false);
        planBtns.add(addBtn); planBtns.add(editBtn);
        planBtns.add(deleteBtn); planBtns.add(refreshBtn);
        plansCard.add(planBtns, BorderLayout.SOUTH);

        // Tiers sub-panel (shown below plans when a FLEXIBLE plan is selected)
        tiersCard = AppShell.createCard();
        tiersCard.setLayout(new BorderLayout());
        tiersCard.setBorder(new EmptyBorder(12, 12, 12, 12));
        tiersTitle = new JLabel("Spend Tiers — select a FLEXIBLE plan");
        tiersTitle.setFont(new Font("SansSerif", Font.BOLD, 13));
        tiersCard.add(tiersTitle, BorderLayout.NORTH);

        tiersModel = new DefaultTableModel(
                new String[]{"Tier ID", "Spend Band", "Rate %"}, 0
        ) { @Override public boolean isCellEditable(int r, int c) { return false; } };

        tiersTable = new JTable(tiersModel);
        configureTable(tiersTable);
        tiersScrollPane = new JScrollPane(tiersTable);
        styleScrollPane(tiersScrollPane);
        tiersScrollPane.setPreferredSize(new Dimension(0, 130));
        tiersCard.add(tiersScrollPane, BorderLayout.CENTER);

        JPanel tierBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        tierBtns.setOpaque(false);
        addTierBtn    = createBtn("Add Tier",    true);
        deleteTierBtn = createBtn("Delete Tier", false);
        tierBtns.add(addTierBtn); tierBtns.add(deleteTierBtn);
        tiersCard.add(tierBtns, BorderLayout.SOUTH);

        leftPanel.add(plansCard, BorderLayout.CENTER);
        leftPanel.add(tiersCard, BorderLayout.SOUTH);

        // ── Right: credits log + month-end button ────────────────────────────
        rightPanel = new JPanel(new BorderLayout(0, 8));
        rightPanel.setOpaque(false);

        creditsCard = AppShell.createCard();
        creditsCard.setLayout(new BorderLayout());
        creditsCard.setBorder(new EmptyBorder(12, 12, 12, 12));

        TitledBorder creditsBorder = BorderFactory.createTitledBorder("Flexible Discount Credits");
        creditsBorder.setTitleFont(new Font("SansSerif", Font.BOLD, 13));
        creditsCard.setBorder(creditsBorder);

        creditsModel = new DefaultTableModel(
                new String[]{"Credit ID", "Cust. ID", "Customer", "Period",
                             "Monthly Spend", "Rate", "Credit", "Remaining"}, 0
        ) { @Override public boolean isCellEditable(int r, int c) { return false; } };

        creditsTable = new JTable(creditsModel);
        configureTable(creditsTable);
        creditsScrollPane = new JScrollPane(creditsTable);
        styleScrollPane(creditsScrollPane);
        creditsCard.add(creditsScrollPane, BorderLayout.CENTER);

        monthEndBtn = createBtn("Run Month-End Discounts", true);
        JPanel meBtnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 6));
        meBtnPanel.setOpaque(false);
        JLabel meHint = new JLabel("Calculates flexible credits for a completed calendar month.");
        meHint.setFont(new Font("SansSerif", Font.PLAIN, 11));
        meBtnPanel.add(meHint);
        meBtnPanel.add(Box.createHorizontalStrut(10));
        meBtnPanel.add(monthEndBtn);
        creditsCard.add(meBtnPanel, BorderLayout.SOUTH);

        rightPanel.add(creditsCard, BorderLayout.CENTER);

        // ── Assemble ─────────────────────────────────────────────────────────
        contentPanel.add(leftPanel,  BorderLayout.WEST);
        contentPanel.add(rightPanel, BorderLayout.CENTER);
        leftPanel.setPreferredSize(new Dimension(440, 0));

        return contentPanel;
    }

    private void wireActions() {
        refreshBtn.addActionListener(e -> { loadPlansTable(); loadCreditsTable(); });

        plansTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) onPlanSelected();
        });

        addBtn.addActionListener(e -> showAddPlanDialog());
        editBtn.addActionListener(e -> {
            int row = plansTable.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(this, "Select a plan to edit."); return; }
            showEditPlanDialog(row);
        });
        deleteBtn.addActionListener(e -> {
            int row = plansTable.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(this, "Select a plan to delete."); return; }
            int planId = (int) plansModel.getValueAt(row, 0);
            String name = (String) plansModel.getValueAt(row, 1);
            int ok = JOptionPane.showConfirmDialog(this,
                    "Delete plan: " + name + "?\nCustomers on this plan will have it removed.",
                    "Confirm Delete", JOptionPane.YES_NO_OPTION);
            if (ok == JOptionPane.YES_OPTION) { DiscountPlanDB.deletePlan(planId); loadPlansTable(); loadCreditsTable(); }
        });

        addTierBtn.addActionListener(e -> {
            int row = plansTable.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(this, "Select a FLEXIBLE plan first."); return; }
            String type = (String) plansModel.getValueAt(row, 2);
            if (!"FLEXIBLE".equals(type)) { JOptionPane.showMessageDialog(this, "Tiers only apply to FLEXIBLE plans."); return; }
            int planId = (int) plansModel.getValueAt(row, 0);
            showAddTierDialog(planId);
        });

        deleteTierBtn.addActionListener(e -> {
            int row = tiersTable.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(this, "Select a tier to delete."); return; }
            int tierId = (int) tiersModel.getValueAt(row, 0);
            DiscountPlanDB.deleteTier(tierId);
            onPlanSelected();
        });

        monthEndBtn.addActionListener(e -> showMonthEndDialog());

        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentShown(java.awt.event.ComponentEvent e) {
                loadPlansTable(); loadCreditsTable();
            }
        });
    }

    // ── Selection ──────────────────────────────────────────────────────────

    private void onPlanSelected() {
        int row = plansTable.getSelectedRow();
        if (row == -1) {
            tiersModel.setRowCount(0);
            tiersTitle.setText("Spend Tiers — select a FLEXIBLE plan");
            return;
        }
        String type   = (String) plansModel.getValueAt(row, 2);
        String name   = (String) plansModel.getValueAt(row, 1);
        int    planId = (int)    plansModel.getValueAt(row, 0);

        if (!"FLEXIBLE".equals(type)) {
            tiersModel.setRowCount(0);
            tiersTitle.setText("Spend Tiers — " + name + " is FIXED (no tiers)");
            return;
        }
        tiersTitle.setText("Spend Tiers — " + name);
        tiersModel.setRowCount(0);
        for (DiscountTier t : DiscountPlanDB.getTiers(planId)) {
            tiersModel.addRow(new Object[]{
                    t.getTierId(),
                    t.bandLabel(),
                    String.format("%.2f%%", t.getRate())
            });
        }
    }

    // ── Table loaders ───────────────────────────────────────────────────────

    private void loadPlansTable() {
        plansModel.setRowCount(0);
        for (DiscountPlan p : DiscountPlanDB.getAllPlans()) {
            plansModel.addRow(new Object[]{
                    p.getDiscountPlanId(),
                    p.getPlanName(),
                    p.getPlanType(),
                    p.isFixed() ? String.format("%.2f%%", p.getDiscountPercent()) : "—",
                    p.getNotes() != null ? p.getNotes() : ""
            });
        }
    }

    private void loadCreditsTable() {
        creditsModel.setRowCount(0);
        for (Object[] row : DiscountPlanDB.getAllCredits()) {
            creditsModel.addRow(row);
        }
    }

    // ── Dialogs ─────────────────────────────────────────────────────────────

    private void showAddPlanDialog() {
        JTextField nameField    = new JTextField();
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"FIXED", "FLEXIBLE"});
        JTextField percentField = new JTextField("0.00");
        JLabel percentNote      = new JLabel("(ignored for FLEXIBLE — use tiers instead)");
        percentNote.setFont(new Font("SansSerif", Font.ITALIC, 11));
        JTextField notesField   = new JTextField();

        Object[] fields = {
                "Plan Name:",        nameField,
                "Plan Type:",        typeCombo,
                "Discount % (FIXED only):", percentField,
                "",                  percentNote,
                "Notes (optional):", notesField
        };

        int result = JOptionPane.showConfirmDialog(this, fields, "Add Discount Plan", JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) return;
        if (nameField.getText().trim().isEmpty()) { JOptionPane.showMessageDialog(this, "Plan name is required."); return; }

        try {
            DiscountPlan plan = new DiscountPlan(
                    nameField.getText().trim(),
                    (String) typeCombo.getSelectedItem(),
                    Double.parseDouble(percentField.getText().trim()),
                    notesField.getText().trim()
            );
            if (DiscountPlanDB.addPlan(plan)) loadPlansTable();
            else JOptionPane.showMessageDialog(this, "Failed to add plan.");
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid discount percentage.");
        }
    }

    private void showEditPlanDialog(int row) {
        int planId = (int) plansModel.getValueAt(row, 0);
        DiscountPlan existing = DiscountPlanDB.getById(planId);
        if (existing == null) return;

        JTextField nameField    = new JTextField(existing.getPlanName());
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"FIXED", "FLEXIBLE"});
        typeCombo.setSelectedItem(existing.getPlanType());
        JTextField percentField = new JTextField(String.valueOf(existing.getDiscountPercent()));
        JTextField notesField   = new JTextField(existing.getNotes() != null ? existing.getNotes() : "");

        Object[] fields = {
                "Plan Name:",  nameField,
                "Plan Type:",  typeCombo,
                "Discount % (FIXED only):", percentField,
                "Notes:",      notesField
        };

        int result = JOptionPane.showConfirmDialog(this, fields, "Edit Discount Plan", JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) return;
        try {
            DiscountPlan updated = new DiscountPlan(planId,
                    nameField.getText().trim(),
                    (String) typeCombo.getSelectedItem(),
                    Double.parseDouble(percentField.getText().trim()),
                    notesField.getText().trim());
            DiscountPlanDB.updatePlan(updated);
            loadPlansTable();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid discount percentage.");
        }
    }

    private void showAddTierDialog(int planId) {
        JTextField minField  = new JTextField("0.00");
        JTextField maxField  = new JTextField("(leave blank for no upper limit)");
        JTextField rateField = new JTextField("1.00");

        Object[] fields = {
                "Min spend £ (inclusive):", minField,
                "Max spend £ (exclusive, blank = unlimited):", maxField,
                "Rate %:", rateField
        };

        int result = JOptionPane.showConfirmDialog(this, fields, "Add Spend Tier", JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) return;

        try {
            double min  = Double.parseDouble(minField.getText().trim());
            String maxTxt = maxField.getText().trim();
            Double max  = (maxTxt.isEmpty() || maxTxt.startsWith("(")) ? null : Double.parseDouble(maxTxt);
            double rate = Double.parseDouble(rateField.getText().trim());
            if (DiscountPlanDB.addTier(planId, min, max, rate)) onPlanSelected();
            else JOptionPane.showMessageDialog(this, "Failed to add tier.");
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter valid numbers.");
        }
    }

    private void showMonthEndDialog() {
        LocalDate last = app.TimeManager.today().minusMonths(1);
        JSpinner yearSpinner  = new JSpinner(new SpinnerNumberModel(last.getYear(), 2020, 2099, 1));
        JSpinner monthSpinner = new JSpinner(new SpinnerNumberModel(last.getMonthValue(), 1, 12, 1));

        Object[] fields = {
                "Year:",  yearSpinner,
                "Month (1–12):", monthSpinner
        };

        int result = JOptionPane.showConfirmDialog(this, fields,
                "Run Month-End Flexible Discounts", JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) return;

        int year  = (int) yearSpinner.getValue();
        int month = (int) monthSpinner.getValue();

        int created = DiscountPlanDB.processMonthEndCredits(year, month);
        loadCreditsTable();

        String msg = created > 0
                ? created + " credit(s) generated for " + year + "-" + String.format("%02d", month)
                    + ".\nCredits will be deducted from each customer's next invoice."
                : "No new credits to generate for " + year + "-" + String.format("%02d", month)
                    + ".\n(Already processed, or no flexible-plan sales in that month.)";
        JOptionPane.showMessageDialog(this, msg,
                "Month-End Complete", JOptionPane.INFORMATION_MESSAGE);
    }

    // ── Table helpers ────────────────────────────────────────────────────────

    private void configureTable(JTable t) {
        t.setRowHeight(40);
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

    private void applyTableTheme(JTable t) {
        if (t == null) return;
        t.setBackground(ThemeManager.tableBackground());
        t.setForeground(ThemeManager.textPrimary());
        t.setGridColor(ThemeManager.tableGrid());
        t.setSelectionBackground(ThemeManager.selectionBackground());
        t.setSelectionForeground(ThemeManager.textPrimary());
        JTableHeader h = t.getTableHeader();
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
        t.setDefaultRenderer(Object.class, r);
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
                    new EmptyBorder(10, 18, 10, 18)));
        }
        return btn;
    }

    @Override
    public void applyTheme() {
        setBackground(ThemeManager.appBackground());
        if (contentPanel != null) contentPanel.setBackground(ThemeManager.appBackground());
        if (leftPanel    != null) leftPanel.setBackground(ThemeManager.appBackground());
        if (rightPanel   != null) rightPanel.setBackground(ThemeManager.appBackground());
        if (tiersCard    != null) tiersCard.setBackground(ThemeManager.panelBackground());
        if (creditsCard  != null) creditsCard.setBackground(ThemeManager.panelBackground());
        if (tiersTitle   != null) tiersTitle.setForeground(ThemeManager.textPrimary());
        if (plansScrollPane  != null) styleScrollPane(plansScrollPane);
        if (tiersScrollPane  != null) styleScrollPane(tiersScrollPane);
        if (creditsScrollPane!= null) styleScrollPane(creditsScrollPane);
        applyTableTheme(plansTable);
        applyTableTheme(tiersTable);
        applyTableTheme(creditsTable);
        repaint(); revalidate();
    }
}
