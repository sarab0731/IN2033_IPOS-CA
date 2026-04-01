package ui;

import app.Session;
import database.CustomerDB;
import database.ProductDB;
import database.RestockOrderDB;
import database.SaleDB;
import database.UserDB;
import domain.RestockOrder;
import domain.User;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class DashboardPanel extends JPanel implements ThemeManager.ThemeListener {

    private JLabel salesTitleValue;
    private JLabel salesSubtitleValue;
    private JLabel salesBottom1;
    private JLabel salesBottom2;

    private JLabel customersTitleValue;
    private JLabel customersSubtitleValue;
    private JLabel customersBottom1;
    private JLabel customersBottom2;

    private JLabel stockTitleValue;
    private JLabel stockSubtitleValue;

    private DefaultTableModel orderTableModel;
    private DefaultTableModel staffTableModel;

    private JPanel orderStatusPanel;
    private JPanel staffPanel;
    private JTable orderTable;
    private JTable staffTable;
    private JScrollPane orderScrollPane;
    private JScrollPane staffScrollPane;
    private JLabel orderStatusTitle;
    private JLabel staffTitle;

    public DashboardPanel(ScreenRouter router) {
        setLayout(new BorderLayout());
        setBackground(ThemeManager.appBackground());

        JPanel dashboardContent = buildDashboardContent(router);

        AppShell shell = new AppShell(
                router,
                MainFrame.SCREEN_DASHBOARD,
                "Welcome back, Username",
                "Administrator",
                dashboardContent
        );

        add(shell, BorderLayout.CENTER);

        ThemeManager.register(this);
        applyTheme();

        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                loadDashboardData();
                User user = Session.getCurrentUser();
                if (user != null) {
                    revalidate();
                    repaint();
                }
            }
        });
    }

    private JPanel buildDashboardContent(ScreenRouter router) {
        JPanel content = new JPanel(new BorderLayout(18, 18));
        content.setOpaque(false);

        content.add(buildTopCards(router), BorderLayout.NORTH);
        content.add(buildBottomSection(), BorderLayout.CENTER);

        return content;
    }

    private JPanel buildTopCards(ScreenRouter router) {
        JPanel cards = new JPanel(new GridLayout(1, 3, 18, 18));
        cards.setOpaque(false);

        cards.add(createSalesCard(router));
        cards.add(createCustomersCard(router));
        cards.add(createStockCard(router));

        return cards;
    }

    private JPanel createSalesCard(ScreenRouter router) {
        JPanel card = createInteractiveCard(true, () -> router.goTo(MainFrame.SCREEN_SALES));

        salesTitleValue = new JLabel("£0.00");
        salesSubtitleValue = new JLabel("0 Orders");
        salesBottom1 = new JLabel("£0.00");
        salesBottom2 = new JLabel("last 14 days");

        configureMainValue(salesTitleValue, true);
        configureSubtitle(salesSubtitleValue, true);
        configureBottomStrong(salesBottom1, true);
        configureBottomMuted(salesBottom2, true);

        populateCard(card, "Total Sales", salesSubtitleValue, salesTitleValue, salesBottom1, salesBottom2, true);
        return card;
    }

    private JPanel createCustomersCard(ScreenRouter router) {
        JPanel card = createInteractiveCard(true, () -> router.goTo(MainFrame.SCREEN_CUSTOMERS));

        customersTitleValue = new JLabel("0");
        customersSubtitleValue = new JLabel("Active customers");
        customersBottom1 = new JLabel("Live");
        customersBottom2 = new JLabel("from database");

        configureMainValue(customersTitleValue, true);
        configureSubtitle(customersSubtitleValue, true);
        configureBottomStrong(customersBottom1, true);
        configureBottomMuted(customersBottom2, true);

        populateCard(card, "Customers", customersSubtitleValue, customersTitleValue, customersBottom1, customersBottom2, true);
        return card;
    }

    private JPanel createStockCard(ScreenRouter router) {
        JPanel card = createInteractiveCard(true, () -> router.goTo(MainFrame.SCREEN_STOCK));

        stockTitleValue = new JLabel("0");
        stockSubtitleValue = new JLabel("0 Low Stock");

        configureMainValue(stockTitleValue, true);
        configureSubtitle(stockSubtitleValue, true);

        JLabel empty1 = new JLabel("");
        JLabel empty2 = new JLabel("");

        populateCard(card, "Stock", stockSubtitleValue, stockTitleValue, empty1, empty2, true);
        return card;
    }

    private JPanel createInteractiveCard(boolean dark, Runnable onClick) {
        Color normalBg = dark ? new Color(30, 32, 38) : ThemeManager.panelBackground();
        Color hoverBg = dark ? new Color(42, 45, 52) : ThemeManager.innerCardBackground();

        JPanel card = AppShell.createCard();
        card.setLayout(new BorderLayout(0, 12));
        card.setPreferredSize(new Dimension(200, 150));
        card.setBackground(normalBg);
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        addCardInteraction(card, normalBg, hoverBg, onClick);
        return card;
    }

    private void populateCard(JPanel card, String title, JLabel subtitleLabel, JLabel valueLabel,
                              JLabel bottom1, JLabel bottom2, boolean dark) {
        Color fg = dark ? Color.WHITE : ThemeManager.textPrimary();

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(fg);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));

        JPanel titleWrap = new JPanel();
        titleWrap.setOpaque(false);
        titleWrap.setLayout(new BoxLayout(titleWrap, BoxLayout.Y_AXIS));
        titleWrap.add(titleLabel);
        titleWrap.add(Box.createVerticalStrut(4));
        titleWrap.add(subtitleLabel);

        JLabel arrow = new JLabel("›");
        arrow.setForeground(fg);
        arrow.setFont(new Font("SansSerif", Font.BOLD, 18));

        top.add(titleWrap, BorderLayout.CENTER);
        top.add(arrow, BorderLayout.EAST);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        bottom.setOpaque(false);
        bottom.add(bottom1);
        bottom.add(bottom2);

        card.add(top, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        card.add(bottom, BorderLayout.SOUTH);
    }

    private void configureMainValue(JLabel label, boolean dark) {
        label.setForeground(dark ? Color.WHITE : ThemeManager.textPrimary());
        label.setFont(new Font("SansSerif", Font.BOLD, 20));
    }

    private void configureSubtitle(JLabel label, boolean dark) {
        label.setForeground(dark ? new Color(180, 180, 180) : ThemeManager.textSecondary());
        label.setFont(new Font("SansSerif", Font.PLAIN, 13));
    }

    private void configureBottomStrong(JLabel label, boolean dark) {
        label.setForeground(dark ? Color.WHITE : ThemeManager.textPrimary());
        label.setFont(new Font("SansSerif", Font.BOLD, 13));
    }

    private void configureBottomMuted(JLabel label, boolean dark) {
        label.setForeground(dark ? new Color(180, 180, 180) : ThemeManager.textSecondary());
        label.setFont(new Font("SansSerif", Font.BOLD, 13));
    }

    private void addCardInteraction(JPanel card, Color normalBg, Color hoverBg, Runnable onClick) {
        java.awt.event.MouseAdapter adapter = new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                card.setBackground(hoverBg);
                card.repaint();
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                card.setBackground(normalBg);
                card.repaint();
            }

            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (onClick != null) {
                    onClick.run();
                }
            }
        };

        card.addMouseListener(adapter);

        for (Component component : card.getComponents()) {
            component.addMouseListener(adapter);

            if (component instanceof Container container) {
                for (Component child : container.getComponents()) {
                    child.addMouseListener(adapter);
                }
            }
        }
    }

    private JPanel buildBottomSection() {
        JPanel bottom = new JPanel(new GridLayout(1, 2, 18, 18));
        bottom.setOpaque(false);

        bottom.add(buildOrderStatusPanel());
        bottom.add(buildStaffPanel());

        return bottom;
    }

    private JPanel buildOrderStatusPanel() {
        orderStatusPanel = AppShell.createCard();
        orderStatusPanel.setLayout(new BorderLayout(12, 12));

        orderStatusTitle = new JLabel("Order Status");
        orderStatusTitle.setFont(new Font("SansSerif", Font.BOLD, 16));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(orderStatusTitle, BorderLayout.WEST);

        String[] columns = {"Order Number", "Merchant", "Status", "Total £", "Created At"};
        orderTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        orderTable = new JTable(orderTableModel);
        styleTable(orderTable);

        orderScrollPane = new JScrollPane(orderTable);
        styleScrollPane(orderScrollPane);

        orderStatusPanel.add(top, BorderLayout.NORTH);
        orderStatusPanel.add(orderScrollPane, BorderLayout.CENTER);

        return orderStatusPanel;
    }

    private JPanel buildStaffPanel() {
        staffPanel = AppShell.createCard();
        staffPanel.setLayout(new BorderLayout(12, 12));

        staffTitle = new JLabel("Staff Accounts");
        staffTitle.setFont(new Font("SansSerif", Font.BOLD, 16));

        String[] columns = {"Name", "Role"};
        staffTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        staffTable = new JTable(staffTableModel);
        styleTable(staffTable);

        staffScrollPane = new JScrollPane(staffTable);
        styleScrollPane(staffScrollPane);

        staffPanel.add(staffTitle, BorderLayout.NORTH);
        staffPanel.add(staffScrollPane, BorderLayout.CENTER);

        return staffPanel;
    }

    private void styleTable(JTable table) {
        table.setRowHeight(36);
        table.setFont(new Font("SansSerif", Font.PLAIN, 12));
        table.setBackground(ThemeManager.tableBackground());
        table.setForeground(ThemeManager.textPrimary());
        table.setSelectionBackground(ThemeManager.selectionBackground());
        table.setSelectionForeground(ThemeManager.textPrimary());
        table.setGridColor(ThemeManager.tableGrid());
        table.setFillsViewportHeight(true);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.getTableHeader().setBackground(ThemeManager.tableHeaderBackground());
        table.getTableHeader().setForeground(ThemeManager.textPrimary());
    }

    private void styleScrollPane(JScrollPane scrollPane) {
        scrollPane.setBorder(BorderFactory.createLineBorder(ThemeManager.borderColor()));
        scrollPane.getViewport().setBackground(ThemeManager.tableBackground());
        scrollPane.setBackground(ThemeManager.tableBackground());
        scrollPane.getVerticalScrollBar().setBackground(ThemeManager.panelBackground());
        scrollPane.getHorizontalScrollBar().setBackground(ThemeManager.panelBackground());
    }

    private void loadDashboardData() {
        int salesCount = SaleDB.getSalesCount();
        double totalSales = SaleDB.getTotalSalesValue();
        double recentSales = SaleDB.getSalesValueLastDays(14);

        int customerCount = CustomerDB.getActiveCustomerCount();
        int productCount = ProductDB.getActiveProductCount();
        int lowStockCount = ProductDB.getLowStockCount();

        salesTitleValue.setText(String.format("£%,.2f", totalSales));
        salesSubtitleValue.setText(salesCount + (salesCount == 1 ? " Order" : " Orders"));
        salesBottom1.setText(String.format("£%,.2f", recentSales));
        salesBottom2.setText("last 14 days");

        customersTitleValue.setText(String.format("%,d", customerCount));
        customersSubtitleValue.setText("Active customers");
        customersBottom1.setText("Live");
        customersBottom2.setText("from database");

        stockTitleValue.setText(String.format("%,d", productCount));
        stockSubtitleValue.setText(lowStockCount + " Low Stock");

        loadOrderTable();
        loadStaffTable();
    }

    private void loadOrderTable() {
        if (orderTableModel == null) {
            return;
        }

        orderTableModel.setRowCount(0);
        List<RestockOrder> orders = RestockOrderDB.getAllOrders();

        int limit = Math.min(orders.size(), 6);
        for (int i = 0; i < limit; i++) {
            RestockOrder o = orders.get(i);
            orderTableModel.addRow(new Object[]{
                    o.getOrderNumber(),
                    o.getMerchantId(),
                    o.getStatus(),
                    String.format("£%.2f", o.getTotalValue()),
                    o.getCreatedAt()
            });
        }
    }

    private void loadStaffTable() {
        if (staffTableModel == null) {
            return;
        }

        staffTableModel.setRowCount(0);
        for (User user : UserDB.getActiveUsers()) {
            staffTableModel.addRow(new Object[]{
                    user.getFullName(),
                    user.getRole()
            });
        }
    }

    @Override
    public void applyTheme() {
        setBackground(ThemeManager.appBackground());

        if (orderStatusPanel != null) {
            orderStatusPanel.setBackground(ThemeManager.panelBackground());
        }

        if (staffPanel != null) {
            staffPanel.setBackground(ThemeManager.panelBackground());
        }

        if (orderStatusTitle != null) {
            orderStatusTitle.setForeground(ThemeManager.textPrimary());
        }

        if (staffTitle != null) {
            staffTitle.setForeground(ThemeManager.textPrimary());
        }

        if (orderTable != null) {
            styleTable(orderTable);
        }

        if (staffTable != null) {
            styleTable(staffTable);
        }

        if (orderScrollPane != null) {
            styleScrollPane(orderScrollPane);
        }

        if (staffScrollPane != null) {
            styleScrollPane(staffScrollPane);
        }

        repaint();
        revalidate();
    }
}