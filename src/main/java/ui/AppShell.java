package ui;

import app.Session;
import domain.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class AppShell extends JPanel implements ThemeManager.ThemeListener {

    private final ScreenRouter router;
    private final String activeScreen;

    private JPanel sidebar;
    private JPanel mainArea;
    private JPanel pageContent;
    private JPanel topBar;

    private JLabel welcomeLabel;
    private JLabel roleLabel;
    private JLabel userNameLabel;
    private JLabel logoLabel;

    private JButton toggleThemeBtn;
    private JButton logoutBtn;

    private final Map<String, JButton> navButtons = new LinkedHashMap<>();

    public AppShell(ScreenRouter router, String activeScreen, String heading, String subheading, JComponent content) {
        this.router = router;
        this.activeScreen = activeScreen;

        setLayout(new BorderLayout());

        sidebar = buildSidebar();
        add(sidebar, BorderLayout.WEST);

        mainArea = new JPanel(new BorderLayout());

        topBar = buildTopBar(heading, subheading);
        mainArea.add(topBar, BorderLayout.NORTH);

        pageContent = new JPanel(new BorderLayout());
        pageContent.setBorder(new EmptyBorder(18, 18, 18, 18));
        pageContent.add(content, BorderLayout.CENTER);

        mainArea.add(pageContent, BorderLayout.CENTER);
        add(mainArea, BorderLayout.CENTER);

        ThemeManager.register(this);
        applyTheme();

        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & java.awt.event.HierarchyEvent.SHOWING_CHANGED) != 0) {
                if (isShowing()) {
                    refreshUserInfo();
                    updateNavVisibility();
                    highlightActiveButton();
                }
            }
        });
    }

    private JPanel buildSidebar() {
        JPanel panel = new JPanel();
        panel.setPreferredSize(new Dimension(180, 0));
        panel.setLayout(new BorderLayout());

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setBorder(new EmptyBorder(22, 20, 20, 20));

        logoLabel = new JLabel();
        setLogo();
        logoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        top.add(logoLabel);
        top.add(Box.createVerticalStrut(28));

        addNavButton(top, "Dashboard", MainFrame.SCREEN_DASHBOARD);
        addNavButton(top, "Stock", MainFrame.SCREEN_STOCK);
        addNavButton(top, "Sales", MainFrame.SCREEN_SALES);
        addNavButton(top, "Customers", MainFrame.SCREEN_CUSTOMERS);
        addNavButton(top, "Online Orders", MainFrame.SCREEN_PU_ORDERS);
        addNavButton(top, "Reports", MainFrame.SCREEN_REPORTS);
        addNavButton(top, "Reminders", MainFrame.SCREEN_REMINDERS);
        addNavButton(top, "Users", MainFrame.SCREEN_USERS);
        addNavButton(top, "Discount Plans", MainFrame.SCREEN_DISCOUNT_PLANS);
        addNavButton(top, "Templates",      MainFrame.SCREEN_TEMPLATES);

        panel.add(top, BorderLayout.NORTH);

        JPanel bottom = new JPanel();
        bottom.setOpaque(false);
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.setBorder(new EmptyBorder(10, 20, 20, 20));

        JLabel help = new JLabel("Help");
        JLabel contact = new JLabel("Contact us");

        help.setAlignmentX(Component.LEFT_ALIGNMENT);
        contact.setAlignmentX(Component.LEFT_ALIGNMENT);

        toggleThemeBtn = createSidebarButton("Toggle Theme", ThemeManager::toggleTheme);
        logoutBtn = createSidebarButton("Log out", () -> {
            Session.logout();
            router.goTo(MainFrame.SCREEN_LOGIN);
        });

        bottom.add(help);
        bottom.add(Box.createVerticalStrut(14));
        bottom.add(contact);
        bottom.add(Box.createVerticalStrut(18));
        bottom.add(toggleThemeBtn);
        bottom.add(Box.createVerticalStrut(10));
        bottom.add(logoutBtn);

        panel.putClientProperty("helpLabel", help);
        panel.putClientProperty("contactLabel", contact);

        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildTopBar(String heading, String subheading) {
        JPanel top = new JPanel(new BorderLayout());
        top.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, ThemeManager.borderColor()),
                new EmptyBorder(24, 28, 24, 28)
        ));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        welcomeLabel = new JLabel(heading);
        welcomeLabel.setFont(new Font("SansSerif", Font.BOLD, 22));

        roleLabel = new JLabel(subheading);
        roleLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));

        left.add(welcomeLabel);
        left.add(Box.createVerticalStrut(8));
        left.add(roleLabel);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        right.setOpaque(false);



        JLabel bell = new JLabel("🔔");
        bell.setFont(new Font("SansSerif", Font.PLAIN, 16));

        JLabel avatar = new JLabel("●");
        avatar.setFont(new Font("SansSerif", Font.BOLD, 18));

        userNameLabel = new JLabel("Username");
        userNameLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));

        right.add(bell);
        right.add(avatar);
        right.add(userNameLabel);

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
        btn.setFont(new Font("SansSerif", Font.BOLD, 14));
        btn.setBorder(new EmptyBorder(10, 14, 10, 14));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (!btn.getBackground().equals(ThemeManager.sidebarActive())) {
                    btn.setBackground(ThemeManager.sidebarHover());
                    btn.setForeground(ThemeManager.textLight());
                }
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                if (!btn.getBackground().equals(ThemeManager.sidebarActive())) {
                    btn.setBackground(ThemeManager.sidebarBackground());
                    btn.setForeground(ThemeManager.textSecondary());
                }
            }
        });

        return btn;
    }

    private JButton createSidebarButton(String text, Runnable action) {
        JButton btn = new JButton(text);
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        btn.setPreferredSize(new Dimension(140, 48));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(true);
        btn.setOpaque(true);
        btn.setFont(new Font("SansSerif", Font.BOLD, 14));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(10, 16, 10, 16));

        applySidebarButtonTheme(btn);

        btn.addActionListener(e -> action.run());

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(ThemeManager.sidebarHover());
                btn.setForeground(ThemeManager.textLight());
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                applySidebarButtonTheme(btn);
            }
        });

        return btn;
    }

    private void applySidebarButtonTheme(JButton btn) {
        btn.setBackground(ThemeManager.sidebarBackground());
        btn.setForeground(ThemeManager.textSecondary());
    }

    private void highlightActiveButton() {
        for (Map.Entry<String, JButton> entry : navButtons.entrySet()) {
            JButton btn = entry.getValue();
            if (entry.getKey().equals(activeScreen)) {
                btn.setBackground(ThemeManager.sidebarActive());
                btn.setForeground(ThemeManager.textLight());
            } else {
                btn.setBackground(ThemeManager.sidebarBackground());
                btn.setForeground(ThemeManager.textSecondary());
            }
        }
    }

    private void refreshUserInfo() {
        User user = Session.getCurrentUser();
        if (user == null) {
            return;
        }

        if (welcomeLabel != null && activeScreen.equals(MainFrame.SCREEN_DASHBOARD)) {
            welcomeLabel.setText("Welcome back, " + user.getFullName());
        }

        if (roleLabel != null) {
            roleLabel.setText(user.getRole());
        }

        if (userNameLabel != null) {
            userNameLabel.setText(user.getUsername());
        }
    }

    private void updateNavVisibility() {
        User user = Session.getCurrentUser();
        if (user == null) {
            return;
        }

        JButton reportsBtn = navButtons.get(MainFrame.SCREEN_REPORTS);
        JButton customersBtn = navButtons.get(MainFrame.SCREEN_CUSTOMERS);
        JButton remindersBtn = navButtons.get(MainFrame.SCREEN_REMINDERS);
        JButton usersBtn = navButtons.get(MainFrame.SCREEN_USERS);
        JButton discountBtn  = navButtons.get(MainFrame.SCREEN_DISCOUNT_PLANS);
        JButton templatesBtn = navButtons.get(MainFrame.SCREEN_TEMPLATES);

        if (reportsBtn != null) {
            reportsBtn.setVisible(user.isManager() || user.isAdmin());
        }
        if (usersBtn != null) {
            usersBtn.setVisible(user.isAdmin());
        }
        if (customersBtn != null) {
            customersBtn.setVisible(!user.isPharmacist());
        }
        if (remindersBtn != null) {
            remindersBtn.setVisible(user.isManager() || user.isAdmin());
        }
        if (discountBtn  != null) {
            discountBtn.setVisible(user.isManager() || user.isAdmin());
        }
        if (templatesBtn != null) {
            templatesBtn.setVisible(user.isManager() || user.isAdmin());
        }
    }

    private void setLogo() {
        try {
            ImageIcon icon = new ImageIcon(getClass().getResource("/logo.png"));
            Image scaled = icon.getImage().getScaledInstance(120, 40, Image.SCALE_SMOOTH);
            logoLabel.setIcon(new ImageIcon(scaled));
            logoLabel.setText("");
        } catch (Exception e) {
            logoLabel.setIcon(null);
            logoLabel.setText("LOGO");
            logoLabel.setForeground(ThemeManager.textLight());
        }
    }

    @Override
    public void applyTheme() {
        setBackground(ThemeManager.appBackground());

        if (sidebar != null) {
            sidebar.setBackground(ThemeManager.sidebarBackground());
            JLabel help = (JLabel) sidebar.getClientProperty("helpLabel");
            JLabel contact = (JLabel) sidebar.getClientProperty("contactLabel");

            if (help != null) {
                help.setForeground(ThemeManager.textSecondary());
            }
            if (contact != null) {
                contact.setForeground(ThemeManager.textSecondary());
            }
        }

        if (mainArea != null) {
            mainArea.setBackground(ThemeManager.appBackground());
        }

        if (pageContent != null) {
            pageContent.setBackground(ThemeManager.appBackground());
        }

        if (topBar != null) {
            topBar.setBackground(ThemeManager.topbarBackground());
            topBar.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, ThemeManager.borderColor()),
                    new EmptyBorder(24, 28, 24, 28)
            ));
        }

        if (welcomeLabel != null) {
            welcomeLabel.setForeground(ThemeManager.textPrimary());
        }

        if (roleLabel != null) {
            roleLabel.setForeground(ThemeManager.textSecondary());
        }



        if (userNameLabel != null) {
            userNameLabel.setForeground(ThemeManager.textPrimary());
        }

        if (toggleThemeBtn != null) {
            applySidebarButtonTheme(toggleThemeBtn);
        }

        if (logoutBtn != null) {
            applySidebarButtonTheme(logoutBtn);
        }

        setLogo();
        highlightActiveButton();

        SwingUtilities.updateComponentTreeUI(this);
        repaint();
        revalidate();
    }

    public static JPanel createCard() {
        JPanel panel = new JPanel();
        panel.setBackground(ThemeManager.panelBackground());

        // Removed white outline border for cleaner dark overview cards
        panel.setBorder(new EmptyBorder(18, 18, 18, 18));

        return panel;
    }
}