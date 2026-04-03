package ui;

import app.Session;
import domain.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class DashboardPanel extends JPanel implements ThemeManager.ThemeListener {

    private final ScreenRouter router;

    private JPanel contentPanel;

    private JPanel salesCard;
    private JPanel customersCard;
    private JPanel stockCard;
    private JPanel orderStatusCard;
    private JPanel staffAccountsCard;

    private JLabel salesTitleLabel;
    private JLabel salesSubLabel;
    private JLabel salesValueLabel;
    private JLabel salesFooterLabel;

    private JLabel customersTitleLabel;
    private JLabel customersSubLabel;
    private JLabel customersValueLabel;
    private JLabel customersFooterLabel;

    private JLabel stockTitleLabel;
    private JLabel stockSubLabel;
    private JLabel stockValueLabel;
    private JLabel stockFooterLabel;

    private JLabel orderStatusTitleLabel;
    private JLabel staffAccountsTitleLabel;

    private JTable orderStatusTable;
    private JTable staffAccountsTable;

    private JScrollPane orderStatusScrollPane;
    private JScrollPane staffAccountsScrollPane;

    public DashboardPanel(ScreenRouter router) {
        this.router = router;

        setLayout(new BorderLayout());
        ThemeManager.register(this);

        AppShell shell = new AppShell(
                router,
                MainFrame.SCREEN_DASHBOARD,
                buildHeading(),
                buildSubheading(),
                buildDashboardContent()
        );

        add(shell, BorderLayout.CENTER);
        applyTheme();
    }

    private String buildHeading() {
        User user = Session.getCurrentUser();
        if (user != null) {
            return "Welcome back, " + user.getUsername();
        }
        return "Welcome back, Username";
    }

    private String buildSubheading() {
        User user = Session.getCurrentUser();
        if (user != null) {
            return user.getRole();
        }
        return "Administrator";
    }

    private JPanel buildDashboardContent() {
        contentPanel = new JPanel(new BorderLayout(20, 20));
        contentPanel.setBorder(new EmptyBorder(8, 8, 8, 8));

        JPanel topCards = new JPanel(new GridLayout(1, 3, 20, 0));
        topCards.setOpaque(false);

        salesCard = createInteractiveCard(MainFrame.SCREEN_SALES);
        customersCard = createInteractiveCard(MainFrame.SCREEN_CUSTOMERS);
        stockCard = createInteractiveCard(MainFrame.SCREEN_STOCK);

        buildSalesCard();
        buildCustomersCard();
        buildStockCard();

        topCards.add(salesCard);
        topCards.add(customersCard);
        topCards.add(stockCard);

        JPanel bottomSection = new JPanel(new GridLayout(1, 2, 20, 0));
        bottomSection.setOpaque(false);

        orderStatusCard = AppShell.createCard();
        orderStatusCard.setLayout(new BorderLayout(12, 12));

        staffAccountsCard = AppShell.createCard();
        staffAccountsCard.setLayout(new BorderLayout(12, 12));

        buildOrderStatusCard();
        buildStaffAccountsCard();

        bottomSection.add(orderStatusCard);
        bottomSection.add(staffAccountsCard);

        contentPanel.add(topCards, BorderLayout.NORTH);
        contentPanel.add(bottomSection, BorderLayout.CENTER);

        return contentPanel;
    }

    private JPanel createInteractiveCard(String targetScreen) {
        JPanel card = AppShell.createCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        setPanelTreeBackground(card, ThemeManager.panelBackground());

        MouseAdapter hoverHandler = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                setPanelTreeBackground(card, ThemeManager.innerCardBackground());
                card.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                Point p = SwingUtilities.convertPoint(
                        e.getComponent(),
                        e.getPoint(),
                        card
                );

                if (!card.contains(p)) {
                    setPanelTreeBackground(card, ThemeManager.panelBackground());
                    card.repaint();
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (targetScreen != null && router != null) {
                    router.goTo(targetScreen);
                }
            }
        };

        card.addMouseListener(hoverHandler);

        return card;
    }

    private void buildSalesCard() {
        salesTitleLabel = new JLabel("Total Sales");
        salesTitleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));

        salesSubLabel = new JLabel("1 Order");
        salesSubLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));

        salesValueLabel = new JLabel("£1,326.13");
        salesValueLabel.setFont(new Font("SansSerif", Font.BOLD, 20));

        salesFooterLabel = new JLabel("£0.00   last 14 days");
        salesFooterLabel.setFont(new Font("SansSerif", Font.BOLD, 13));

        JLabel arrow = new JLabel("›");
        arrow.setFont(new Font("SansSerif", Font.BOLD, 24));
        arrow.setAlignmentX(Component.RIGHT_ALIGNMENT);

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(true);
        topRow.add(salesTitleLabel, BorderLayout.WEST);
        topRow.add(arrow, BorderLayout.EAST);

        salesCard.add(topRow);
        salesCard.add(Box.createVerticalStrut(6));
        salesCard.add(salesSubLabel);
        salesCard.add(Box.createVerticalStrut(22));
        salesCard.add(salesValueLabel);
        salesCard.add(Box.createVerticalGlue());
        salesCard.add(salesFooterLabel);
    }

    private void buildCustomersCard() {
        customersTitleLabel = new JLabel("Customers");
        customersTitleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));

        customersSubLabel = new JLabel("Active customers");
        customersSubLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));

        customersValueLabel = new JLabel("0");
        customersValueLabel.setFont(new Font("SansSerif", Font.BOLD, 20));

        customersFooterLabel = new JLabel("Live   from database");
        customersFooterLabel.setFont(new Font("SansSerif", Font.BOLD, 13));

        JLabel arrow = new JLabel("›");
        arrow.setFont(new Font("SansSerif", Font.BOLD, 24));
        arrow.setAlignmentX(Component.RIGHT_ALIGNMENT);

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(true);
        topRow.add(customersTitleLabel, BorderLayout.WEST);
        topRow.add(arrow, BorderLayout.EAST);

        customersCard.add(topRow);
        customersCard.add(Box.createVerticalStrut(6));
        customersCard.add(customersSubLabel);
        customersCard.add(Box.createVerticalStrut(22));
        customersCard.add(customersValueLabel);
        customersCard.add(Box.createVerticalGlue());
        customersCard.add(customersFooterLabel);
    }

    private void buildStockCard() {
        stockTitleLabel = new JLabel("Stock");
        stockTitleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));

        stockSubLabel = new JLabel("0 Low Stock");
        stockSubLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));

        stockValueLabel = new JLabel("3");
        stockValueLabel.setFont(new Font("SansSerif", Font.BOLD, 20));

        stockFooterLabel = new JLabel("");
        stockFooterLabel.setFont(new Font("SansSerif", Font.BOLD, 13));

        JLabel arrow = new JLabel("›");
        arrow.setFont(new Font("SansSerif", Font.BOLD, 24));
        arrow.setAlignmentX(Component.RIGHT_ALIGNMENT);

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(true);
        topRow.add(stockTitleLabel, BorderLayout.WEST);
        topRow.add(arrow, BorderLayout.EAST);

        stockCard.add(topRow);
        stockCard.add(Box.createVerticalStrut(6));
        stockCard.add(stockSubLabel);
        stockCard.add(Box.createVerticalStrut(22));
        stockCard.add(stockValueLabel);
        stockCard.add(Box.createVerticalGlue());
        stockCard.add(stockFooterLabel);
    }

    private void buildOrderStatusCard() {
        orderStatusTitleLabel = new JLabel("Order Status");
        orderStatusTitleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));

        String[] columns = {"Order Number", "Merchant", "Status", "Total £", "Created At"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        orderStatusTable = new JTable(model);
        configureTable(orderStatusTable);

        orderStatusScrollPane = new JScrollPane(orderStatusTable);
        styleScrollPane(orderStatusScrollPane);

        orderStatusCard.add(orderStatusTitleLabel, BorderLayout.NORTH);
        orderStatusCard.add(orderStatusScrollPane, BorderLayout.CENTER);
    }

    private void buildStaffAccountsCard() {
        staffAccountsTitleLabel = new JLabel("Staff Accounts");
        staffAccountsTitleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));

        String[] columns = {"Name", "Role"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        model.addRow(new Object[]{"Administrator", "ADMIN"});
        model.addRow(new Object[]{"John Pharmacist", "PHARMACIST"});
        model.addRow(new Object[]{"Store Manager", "MANAGER"});

        staffAccountsTable = new JTable(model);
        configureTable(staffAccountsTable);

        staffAccountsScrollPane = new JScrollPane(staffAccountsTable);
        styleScrollPane(staffAccountsScrollPane);

        staffAccountsCard.add(staffAccountsTitleLabel, BorderLayout.NORTH);
        staffAccountsCard.add(staffAccountsScrollPane, BorderLayout.CENTER);
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
        header.setResizingAllowed(true);
        header.setFont(new Font("SansSerif", Font.BOLD, 13));
        header.setBorder(BorderFactory.createEmptyBorder());

        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.setBorder(new EmptyBorder(0, 10, 0, 10));
        renderer.setHorizontalAlignment(SwingConstants.LEFT);
        table.setDefaultRenderer(Object.class, renderer);
    }

    private void styleScrollPane(JScrollPane scrollPane) {
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBorder(null);
        scrollPane.getViewport().setOpaque(true);
        scrollPane.getViewport().setBackground(ThemeManager.tableBackground());
        scrollPane.setBackground(ThemeManager.tableBackground());
        scrollPane.setOpaque(true);

        if (scrollPane.getVerticalScrollBar() != null) {
            scrollPane.getVerticalScrollBar().setBackground(ThemeManager.panelBackground());
            scrollPane.getVerticalScrollBar().setBorder(BorderFactory.createEmptyBorder());
        }

        if (scrollPane.getHorizontalScrollBar() != null) {
            scrollPane.getHorizontalScrollBar().setBackground(ThemeManager.panelBackground());
            scrollPane.getHorizontalScrollBar().setBorder(BorderFactory.createEmptyBorder());
        }
    }

    private void setPanelTreeBackground(Component component, Color color) {
        if (component instanceof JPanel) {
            component.setBackground(color);
        }

        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                setPanelTreeBackground(child, color);
            }
        }
    }

    @Override
    public void applyTheme() {
        setBackground(ThemeManager.appBackground());

        if (contentPanel != null) {
            contentPanel.setBackground(ThemeManager.appBackground());
        }

        applyCardTheme(salesCard);
        applyCardTheme(customersCard);
        applyCardTheme(stockCard);
        applyCardTheme(orderStatusCard);
        applyCardTheme(staffAccountsCard);

        applyLabelPrimary(salesTitleLabel);
        applyLabelSecondary(salesSubLabel);
        applyLabelPrimary(salesValueLabel);
        applyLabelSecondary(salesFooterLabel);

        applyLabelPrimary(customersTitleLabel);
        applyLabelSecondary(customersSubLabel);
        applyLabelPrimary(customersValueLabel);
        applyLabelSecondary(customersFooterLabel);

        applyLabelPrimary(stockTitleLabel);
        applyLabelSecondary(stockSubLabel);
        applyLabelPrimary(stockValueLabel);
        applyLabelSecondary(stockFooterLabel);

        applyLabelPrimary(orderStatusTitleLabel);
        applyLabelPrimary(staffAccountsTitleLabel);

        applyTableTheme(orderStatusTable);
        applyTableTheme(staffAccountsTable);

        if (orderStatusScrollPane != null) {
            styleScrollPane(orderStatusScrollPane);
        }

        if (staffAccountsScrollPane != null) {
            styleScrollPane(staffAccountsScrollPane);
        }

        repaint();
        revalidate();
    }

    private void applyCardTheme(JPanel panel) {
        if (panel != null) {
            setPanelTreeBackground(panel, ThemeManager.panelBackground());
        }
    }

    private void applyLabelPrimary(JLabel label) {
        if (label != null) {
            label.setForeground(ThemeManager.textPrimary());
        }
    }

    private void applyLabelSecondary(JLabel label) {
        if (label != null) {
            label.setForeground(ThemeManager.textSecondary());
        }
    }

    private void applyTableTheme(JTable table) {
        if (table == null) {
            return;
        }

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
        }

        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.setBackground(ThemeManager.tableBackground());
        renderer.setForeground(ThemeManager.textPrimary());
        renderer.setBorder(new EmptyBorder(0, 10, 0, 10));
        renderer.setHorizontalAlignment(SwingConstants.LEFT);
        table.setDefaultRenderer(Object.class, renderer);
    }
}