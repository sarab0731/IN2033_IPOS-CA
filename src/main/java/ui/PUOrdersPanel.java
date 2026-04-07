package ui;

import domain.PUOrder;
import domain.PUOrder.PUOrderItem;
import integration.PUApiClient;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.List;

public class PUOrdersPanel extends JPanel implements ThemeManager.ThemeListener {

    private final ScreenRouter router;

    private JPanel contentPanel;
    private JPanel headerPanel;
    private JPanel tableCard;
    private JPanel bottomControls;

    private JTable table;
    private JScrollPane scrollPane;
    private DefaultTableModel tableModel;

    private JComboBox<String> statusFilter;
    private JButton refreshBtn;
    private JButton viewItemsBtn;
    private JButton advanceStatusBtn;

    private JLabel titleLabel;
    private JLabel statusNote;

    private List<PUOrder> currentOrders;

    public PUOrdersPanel(ScreenRouter router) {
        this.router = router;

        setLayout(new BorderLayout());
        ThemeManager.register(this);

        AppShell shell = new AppShell(
                router,
                MainFrame.SCREEN_PU_ORDERS,
                "Online Orders",
                "Manage online orders",
                buildContent()
        );

        add(shell, BorderLayout.CENTER);
        wireActions();
        applyTheme();
    }

    private JPanel buildContent() {
        contentPanel = new JPanel(new BorderLayout(20, 20));
        contentPanel.setBorder(new EmptyBorder(8, 8, 8, 8));

        // Header row
        headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        titleLabel = new JLabel("Orders");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));

        JPanel rightHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        rightHeader.setOpaque(false);

        statusFilter = new JComboBox<>(new String[]{
                "All", "ACCEPTED", "READY_FOR_SHIPMENT", "SHIPPED"
        });
        statusFilter.setPreferredSize(new Dimension(190, 36));

        refreshBtn = createBtn("Refresh", false);
        rightHeader.add(new JLabel("Filter:"));
        rightHeader.add(statusFilter);
        rightHeader.add(refreshBtn);

        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(rightHeader, BorderLayout.EAST);

        // Table
        tableCard = AppShell.createCard();
        tableCard.setLayout(new BorderLayout());

        tableModel = new DefaultTableModel(
                new String[]{"Order ID", "Member", "Delivery Address", "Status", "Total £"}, 0
        ) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        table = new JTable(tableModel);
        configureTable(table);

        scrollPane = new JScrollPane(table);
        styleScrollPane(scrollPane);
        tableCard.add(scrollPane, BorderLayout.CENTER);

        statusNote = new JLabel(" ");
        statusNote.setFont(new Font("SansSerif", Font.ITALIC, 12));
        tableCard.add(statusNote, BorderLayout.SOUTH);

        // Bottom buttons
        bottomControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        bottomControls.setOpaque(false);

        viewItemsBtn    = createBtn("View Items", false);
        advanceStatusBtn = createBtn("Advance Status", true);

        bottomControls.add(viewItemsBtn);
        bottomControls.add(advanceStatusBtn);

        contentPanel.add(headerPanel,    BorderLayout.NORTH);
        contentPanel.add(tableCard,      BorderLayout.CENTER);
        contentPanel.add(bottomControls, BorderLayout.SOUTH);

        return contentPanel;
    }

    private void wireActions() {
        refreshBtn.addActionListener(e -> loadOrders());
        statusFilter.addActionListener(e -> applyFilter());

        viewItemsBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(this, "Please select an order."); return; }
            PUOrder order = currentOrders.get(table.convertRowIndexToModel(row));
            showItemsDialog(order);
        });

        advanceStatusBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(this, "Please select an order."); return; }
            PUOrder order = currentOrders.get(table.convertRowIndexToModel(row));
            advanceStatus(order);
        });

        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentShown(java.awt.event.ComponentEvent e) { loadOrders(); }
        });
    }

    private void loadOrders() {
        statusNote.setText("Fetching from PU system…");
        tableModel.setRowCount(0);

        SwingWorker<List<PUOrder>, Void> worker = new SwingWorker<>() {
            @Override protected List<PUOrder> doInBackground() {
                return PUApiClient.getOnlineOrders();
            }
            @Override protected void done() {
                try {
                    currentOrders = get();
                    applyFilter();
                    statusNote.setText(currentOrders.isEmpty()
                            ? "No new orders found."
                            : currentOrders.size() + " order(s) loaded.");
                } catch (Exception ex) {
                    statusNote.setText("Failed to load orders from PU system.");
                }
            }
        };
        worker.execute();
    }

    private void applyFilter() {
        if (currentOrders == null) return;
        tableModel.setRowCount(0);
        String filter = (String) statusFilter.getSelectedItem();

        for (PUOrder o : currentOrders) {
            if ("All".equals(filter) || o.getStatus().equals(filter)) {
                tableModel.addRow(new Object[]{
                        o.getOrderId(),
                        o.getMemberName(),
                        o.getDeliveryAddress(),
                        o.getStatus(),
                        String.format("%.2f", o.getTotalValue())
                });
            }
        }
    }

    private void advanceStatus(PUOrder order) {
        String next = nextStatus(order.getStatus());
        if (next == null) {
            JOptionPane.showMessageDialog(this, "This order is already delivered.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Advance order " + order.getOrderId()
                + "\nfrom " + order.getStatus() + " → " + next + "?",
                "Confirm", JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) return;

        boolean ok = PUApiClient.updateOrderStatus(order.getOrderId(), next);
        if (ok) {
            JOptionPane.showMessageDialog(this, "Status updated to " + next + ".");
            loadOrders();
        } else {
            JOptionPane.showMessageDialog(this,
                    "Failed to update status. Check that the PU system is running.");
        }
    }

    private void showItemsDialog(PUOrder order) {
        DefaultTableModel itemModel = new DefaultTableModel(
                new String[]{"Product", "Qty", "Unit Price £", "Line Total £"}, 0
        ) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        double total = 0;
        for (PUOrderItem item : order.getItems()) {
            double lineTotal = item.getQuantity() * item.getUnitPrice();
            total += lineTotal;
            itemModel.addRow(new Object[]{
                    item.getProductName(),
                    item.getQuantity(),
                    String.format("%.2f", item.getUnitPrice()),
                    String.format("%.2f", lineTotal)
            });
        }

        JTable itemTable = new JTable(itemModel);
        configureTable(itemTable);
        applyTableTheme(itemTable);
        JScrollPane scroll = new JScrollPane(itemTable);
        styleScrollPane(scroll);
        scroll.setPreferredSize(new Dimension(500, 220));

        JLabel totalLbl = new JLabel("Total: £" + String.format("%.2f", total));
        totalLbl.setFont(new Font("SansSerif", Font.BOLD, 13));
        totalLbl.setBorder(new EmptyBorder(8, 4, 0, 0));

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));
        panel.setBackground(ThemeManager.panelBackground());
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(totalLbl, BorderLayout.SOUTH);

        JOptionPane.showMessageDialog(this, panel,
                "Items — Order " + order.getOrderId(), JOptionPane.PLAIN_MESSAGE);
    }

    private String nextStatus(String current) {
        return switch (current) {
            case "ACCEPTED"           -> "READY_FOR_SHIPMENT";
            case "READY_FOR_SHIPMENT" -> "SHIPPED";
            case "SHIPPED"            -> "DELIVERED";
            default                   -> null;
        };
    }

    // -------------------------------------------------------------------------
    // UI helpers
    // -------------------------------------------------------------------------

    private JButton createBtn(String text, boolean primary) {
        JButton btn = new JButton(text);
        btn.setFocusPainted(false);
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
        if (primary) {
            btn.setBackground(ThemeManager.buttonDark());
            btn.setForeground(ThemeManager.textLight());
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

    private void configureTable(JTable t) {
        t.setRowHeight(44);
        t.setShowGrid(true);
        t.setIntercellSpacing(new Dimension(1, 1));
        t.setFillsViewportHeight(true);
        t.setBorder(BorderFactory.createEmptyBorder());
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setDefaultEditor(Object.class, null);

        JTableHeader header = t.getTableHeader();
        header.setReorderingAllowed(false);
        header.setFont(new Font("SansSerif", Font.BOLD, 13));
        header.setBorder(BorderFactory.createEmptyBorder());

        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.setHorizontalAlignment(SwingConstants.LEFT);
        renderer.setBorder(new EmptyBorder(0, 10, 0, 10));
        t.setDefaultRenderer(Object.class, renderer);
    }

    private void styleScrollPane(JScrollPane sp) {
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setOpaque(true);
        sp.setBackground(ThemeManager.tableBackground());
        sp.getViewport().setBackground(ThemeManager.tableBackground());
        sp.getViewport().setBorder(null);
    }

    private void applyTableTheme(JTable t) {
        t.setBackground(ThemeManager.tableBackground());
        t.setForeground(ThemeManager.textPrimary());
        t.setGridColor(ThemeManager.tableGrid());
        t.setSelectionBackground(ThemeManager.selectionBackground());
        t.setSelectionForeground(ThemeManager.textPrimary());

        JTableHeader header = t.getTableHeader();
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
        t.setDefaultRenderer(Object.class, renderer);
    }

    @Override
    public void applyTheme() {
        setBackground(ThemeManager.appBackground());
        if (contentPanel  != null) contentPanel.setBackground(ThemeManager.appBackground());
        if (headerPanel   != null) headerPanel.setBackground(ThemeManager.appBackground());
        if (bottomControls!= null) bottomControls.setBackground(ThemeManager.appBackground());
        if (tableCard     != null) tableCard.setBackground(ThemeManager.panelBackground());
        if (titleLabel    != null) titleLabel.setForeground(ThemeManager.textPrimary());
        if (statusNote    != null) statusNote.setForeground(ThemeManager.textSecondary());
        if (table         != null) applyTableTheme(table);
        if (scrollPane    != null) styleScrollPane(scrollPane);
        if (refreshBtn    != null) {
            refreshBtn.setBackground(ThemeManager.buttonLight());
            refreshBtn.setForeground(ThemeManager.textPrimary());
        }
        if (advanceStatusBtn != null) {
            advanceStatusBtn.setBackground(ThemeManager.buttonDark());
            advanceStatusBtn.setForeground(ThemeManager.textLight());
        }
        if (viewItemsBtn != null) {
            viewItemsBtn.setBackground(ThemeManager.buttonLight());
            viewItemsBtn.setForeground(ThemeManager.textPrimary());
        }
        repaint();
        revalidate();
    }
}
