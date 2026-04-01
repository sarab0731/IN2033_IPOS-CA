package ui;

import app.Session;
import domain.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class AppShell extends JPanel {

    private final ScreenRouter router;
    private final String activeScreen;

    private final JPanel sidebar;
    private final JPanel pageContent;
    private JLabel welcomeLabel;
    private JLabel roleLabel;

    private final Map<String, JButton> navButtons = new LinkedHashMap<>();

    private static final Color BG = new Color(242, 242, 242);
    private static final Color SIDEBAR = new Color(28, 30, 35);
    private static final Color SIDEBAR_HOVER = new Color(44, 47, 54);
    private static final Color SIDEBAR_ACTIVE = new Color(56, 60, 68);
    private static final Color TEXT_LIGHT = new Color(245, 245, 245);
    private static final Color TEXT_MUTED = new Color(165, 170, 176);
    private static final Color CARD = new Color(255, 255, 255);
    private static final Color BORDER = new Color(225, 225, 225);
    private static final Color TEXT_DARK = new Color(40, 40, 40);

    public AppShell(ScreenRouter router, String activeScreen, String heading, String subheading, JComponent content) {
        this.router = router;
        this.activeScreen = activeScreen;

        setLayout(new BorderLayout());
        setBackground(BG);

        sidebar = buildSidebar();
        add(sidebar, BorderLayout.WEST);

        JPanel mainArea = new JPanel(new BorderLayout());
        mainArea.setBackground(BG);

        JPanel topBar = buildTopBar(heading, subheading);
        mainArea.add(topBar, BorderLayout.NORTH);

        pageContent = new JPanel(new BorderLayout());
        pageContent.setBackground(BG);
        pageContent.setBorder(new EmptyBorder(18, 18, 18, 18));
        pageContent.add(content, BorderLayout.CENTER);

        mainArea.add(pageContent, BorderLayout.CENTER);
        add(mainArea, BorderLayout.CENTER);

        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                refreshUserInfo();
                updateNavVisibility();
                highlightActiveButton();
            }
        });
    }

    private JPanel buildSidebar() {
        JPanel panel = new JPanel();
        panel.setPreferredSize(new Dimension(180, 0));
        panel.setBackground(SIDEBAR);
        panel.setLayout(new BorderLayout());

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setBorder(new EmptyBorder(22, 20, 20, 20));

        JLabel logo = new JLabel("<html><div style='font-size:24px; font-weight:bold;'>SWIFT</div>" +
                "<div style='font-size:11px; letter-spacing:2px;'>SOLUTIONS</div></html>");
        logo.setForeground(TEXT_LIGHT);
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(logo);
        top.add(Box.createVerticalStrut(28));

        addNavButton(top, "Overview", MainFrame.SCREEN_DASHBOARD);
        addNavButton(top, "Stock", MainFrame.SCREEN_STOCK);
        addNavButton(top, "Sales", MainFrame.SCREEN_SALES);
        addNavButton(top, "Customers", MainFrame.SCREEN_CUSTOMERS);
        addNavButton(top, "Orders", MainFrame.SCREEN_ORDERS);
        addNavButton(top, "Reports", MainFrame.SCREEN_REPORTS);
        addNavButton(top, "Reminders", MainFrame.SCREEN_REMINDERS);

        panel.add(top, BorderLayout.NORTH);

        JPanel bottom = new JPanel();
        bottom.setOpaque(false);
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.setBorder(new EmptyBorder(10, 20, 20, 20));

        JLabel help = new JLabel("Help");
        help.setForeground(TEXT_MUTED);
        help.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel contact = new JLabel("Contact us");
        contact.setForeground(TEXT_MUTED);
        contact.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton logoutBtn = createNavButton("Log out");
        logoutBtn.addActionListener(e -> {
            Session.logout();
            router.goTo(MainFrame.SCREEN_LOGIN);
        });

        bottom.add(help);
        bottom.add(Box.createVerticalStrut(14));
        bottom.add(contact);
        bottom.add(Box.createVerticalStrut(18));
        bottom.add(logoutBtn);

        panel.add(bottom, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel buildTopBar(String heading, String subheading) {
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(new Color(250, 250, 250));
        top.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                new EmptyBorder(24, 28, 24, 28)
        ));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        welcomeLabel = new JLabel(heading);
        welcomeLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        welcomeLabel.setForeground(TEXT_DARK);

        roleLabel = new JLabel(subheading);
        roleLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        roleLabel.setForeground(new Color(120, 120, 120));

        left.add(welcomeLabel);
        left.add(Box.createVerticalStrut(8));
        left.add(roleLabel);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        right.setOpaque(false);

        JTextField search = new JTextField("Search", 12);
        search.setPreferredSize(new Dimension(150, 36));
        search.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(225, 225, 225)),
                new EmptyBorder(6, 12, 6, 12)
        ));
        search.setBackground(new Color(245, 245, 245));
        search.setForeground(new Color(130, 130, 130));

        JLabel bell = new JLabel("\uD83D\uDD14");
        bell.setFont(new Font("SansSerif", Font.PLAIN, 16));

        JLabel avatar = new JLabel("\u25CF");
        avatar.setFont(new Font("SansSerif", Font.BOLD, 18));
        avatar.setForeground(new Color(110, 110, 110));

        JLabel userName = new JLabel("Username");
        userName.setForeground(TEXT_DARK);
        userName.setFont(new Font("SansSerif", Font.PLAIN, 13));

        right.add(search);
        right.add(bell);
        right.add(avatar);
        right.add(userName);

        top.add(left, BorderLayout.WEST);
        top.add(right, BorderLayout.EAST);

        return top;
    }

    private void addNavButton(JPanel container, String text, String screen) {
        JButton btn = createNavButton(text);
        btn.addActionListener(e -> router.goTo(screen));
        navButtons.put(screen, btn);
        container.add(btn);
        container.add(Box.createVerticalStrut(8));
    }

    private JButton createNavButton(String text) {
        JButton btn = new JButton(text);
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btn.setPreferredSize(new Dimension(140, 40));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(true);
        btn.setOpaque(true);
        btn.setBackground(SIDEBAR);
        btn.setForeground(TEXT_MUTED);
        btn.setFont(new Font("SansSerif", Font.BOLD, 14));
        btn.setBorder(new EmptyBorder(10, 14, 10, 14));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (btn.getBackground() != SIDEBAR_ACTIVE) {
                    btn.setBackground(SIDEBAR_HOVER);
                    btn.setForeground(TEXT_LIGHT);
                }
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                if (btn.getBackground() != SIDEBAR_ACTIVE) {
                    btn.setBackground(SIDEBAR);
                    btn.setForeground(TEXT_MUTED);
                }
            }
        });

        return btn;
    }

    private void highlightActiveButton() {
        for (Map.Entry<String, JButton> entry : navButtons.entrySet()) {
            JButton btn = entry.getValue();
            if (entry.getKey().equals(activeScreen)) {
                btn.setBackground(SIDEBAR_ACTIVE);
                btn.setForeground(TEXT_LIGHT);
            } else {
                btn.setBackground(SIDEBAR);
                btn.setForeground(TEXT_MUTED);
            }
        }
    }

    private void refreshUserInfo() {
        User user = Session.getCurrentUser();
        if (user == null) {
            return;
        }

        if (activeScreen.equals(MainFrame.SCREEN_DASHBOARD)) {
            welcomeLabel.setText("Welcome back, " + user.getFullName());
        }

        roleLabel.setText(user.getRole());
    }

    private void updateNavVisibility() {
        User user = Session.getCurrentUser();
        if (user == null) {
            return;
        }

        JButton reportsBtn = navButtons.get(MainFrame.SCREEN_REPORTS);
        JButton customersBtn = navButtons.get(MainFrame.SCREEN_CUSTOMERS);
        JButton ordersBtn = navButtons.get(MainFrame.SCREEN_ORDERS);
        JButton remindersBtn = navButtons.get(MainFrame.SCREEN_REMINDERS);

        if (reportsBtn != null) {
            reportsBtn.setVisible(user.isManager() || user.isAdmin());
        }
        if (customersBtn != null) {
            customersBtn.setVisible(!user.isPharmacist());
        }
        if (ordersBtn != null) {
            ordersBtn.setVisible(!user.isPharmacist());
        }
        if (remindersBtn != null) {
            remindersBtn.setVisible(user.isManager() || user.isAdmin());
        }
    }

    public static JPanel createCard() {
        JPanel panel = new JPanel();
        panel.setBackground(CARD);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(232, 232, 232)),
                new EmptyBorder(18, 18, 18, 18)
        ));
        return panel;
    }
}