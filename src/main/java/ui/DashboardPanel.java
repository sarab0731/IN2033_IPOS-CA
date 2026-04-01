package ui;

import app.Session;
import domain.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class DashboardPanel extends JPanel {

    public DashboardPanel(ScreenRouter router) {
        setLayout(new BorderLayout());
        setBackground(new Color(242, 242, 242));

        JPanel dashboardContent = buildDashboardContent(router);

        AppShell shell = new AppShell(
                router,
                MainFrame.SCREEN_DASHBOARD,
                "Welcome back, Username",
                "Administrator",
                dashboardContent
        );

        add(shell, BorderLayout.CENTER);

        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
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

        cards.add(createStatCard(
                "Total Sales",
                "731 Orders",
                "£9,328.55",
                "+15.6%",
                "+1.4k this week",
                true,
                () -> router.goTo(MainFrame.SCREEN_SALES)
        ));

        cards.add(createStatCard(
                "Customers",
                "Avg. time: 4:30m",
                "12,302",
                "+12.7%",
                "+1.2k this week",
                false,
                () -> router.goTo(MainFrame.SCREEN_CUSTOMERS)
        ));

        cards.add(createStatCard(
                "Stock",
                "2 Low Stock",
                "963",
                "",
                "",
                false,
                () -> router.goTo(MainFrame.SCREEN_STOCK)
        ));

        return cards;
    }

    private JPanel createStatCard(
            String title,
            String subtitle,
            String value,
            String stat1,
            String stat2,
            boolean dark,
            Runnable onClick
    ) {
        Color normalBg = dark ? new Color(30, 32, 38) : Color.WHITE;
        Color hoverBg = dark ? new Color(42, 45, 52) : new Color(245, 245, 245);

        JPanel card = AppShell.createCard();
        card.setLayout(new BorderLayout(0, 12));
        card.setPreferredSize(new Dimension(200, 150));
        card.setBackground(normalBg);
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        Color fg = dark ? Color.WHITE : new Color(35, 35, 35);
        Color sub = dark ? new Color(180, 180, 180) : new Color(130, 130, 130);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(fg);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));

        JLabel subLabel = new JLabel(subtitle);
        subLabel.setForeground(sub);
        subLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));

        JPanel titleWrap = new JPanel();
        titleWrap.setOpaque(false);
        titleWrap.setLayout(new BoxLayout(titleWrap, BoxLayout.Y_AXIS));
        titleWrap.add(titleLabel);
        titleWrap.add(Box.createVerticalStrut(4));
        titleWrap.add(subLabel);

        JLabel arrow = new JLabel("›");
        arrow.setForeground(fg);
        arrow.setFont(new Font("SansSerif", Font.BOLD, 18));

        top.add(titleWrap, BorderLayout.CENTER);
        top.add(arrow, BorderLayout.EAST);

        JLabel valueLabel = new JLabel(value);
        valueLabel.setForeground(fg);
        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 20));

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        bottom.setOpaque(false);

        if (!stat1.isEmpty()) {
            JLabel s1 = new JLabel(stat1);
            s1.setForeground(fg);
            s1.setFont(new Font("SansSerif", Font.BOLD, 13));
            bottom.add(s1);
        }

        if (!stat2.isEmpty()) {
            JLabel s2 = new JLabel(stat2);
            s2.setForeground(sub);
            s2.setFont(new Font("SansSerif", Font.BOLD, 13));
            bottom.add(s2);
        }

        card.add(top, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        card.add(bottom, BorderLayout.SOUTH);

        addCardInteraction(card, normalBg, hoverBg, onClick);

        return card;
    }

    private void addCardInteraction(JPanel card, Color normalBg, Color hoverBg, Runnable onClick) {
        MouseAdapter adapter = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                card.setBackground(hoverBg);
                card.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                card.setBackground(normalBg);
                card.repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
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
        JPanel panel = AppShell.createCard();
        panel.setLayout(new BorderLayout(12, 12));

        JLabel title = new JLabel("Order Status");
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        title.setForeground(new Color(45, 45, 45));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(title, BorderLayout.WEST);

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        filters.setOpaque(false);

        JComboBox<String> orderFilter = new JComboBox<>(new String[]{"All orders", "In Transit", "Complete", "Placed"});
        JComboBox<String> dateFilter = new JComboBox<>(new String[]{"Last 14 Days", "Last 30 Days", "This Month"});
        filters.add(orderFilter);
        filters.add(dateFilter);

        top.add(filters, BorderLayout.EAST);

        String[] columns = {"Order", "Delivery Date", "Quantity", "Status", "Notes"};
        Object[][] data = {
                {"Order 1", "DD/MM/YYYY", "Value", "In Transit", "Extra Info"},
                {"Order 2", "DD/MM/YYYY", "Value", "Complete", "Extra Info"},
                {"Order 3", "DD/MM/YYYY", "Value", "Order Placed", "Extra Info"},
                {"Order 4", "DD/MM/YYYY", "Value", "Complete", "Extra Info"},
                {"Order 5", "DD/MM/YYYY", "Value", "Not Ordered", "Extra Info"},
                {"Order 6", "DD/MM/YYYY", "Value", "Order Placed", "Extra Info"}
        };

        JTable table = new JTable(data, columns);
        table.setRowHeight(36);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.setFont(new Font("SansSerif", Font.PLAIN, 12));

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        panel.add(top, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel buildStaffPanel() {
        JPanel panel = AppShell.createCard();
        panel.setLayout(new BorderLayout(12, 12));
        panel.setPreferredSize(new Dimension(250, 0));

        JLabel title = new JLabel("Staff Accounts");
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        title.setForeground(new Color(45, 45, 45));

        String[] columns = {"Staff"};
        Object[][] data = {
                {"Staff 1"},
                {"Staff 2"},
                {"Staff 3"},
                {"Staff 4"},
                {"Staff 5"},
                {"Staff 6"}
        };

        JTable table = new JTable(data, columns);
        table.setRowHeight(36);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        panel.add(title, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }
}