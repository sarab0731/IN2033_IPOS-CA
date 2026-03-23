package ui;

import app.Session;
import domain.User;

import javax.swing.*;
import java.awt.*;

public class DashboardPanel extends JPanel {

    public DashboardPanel(ScreenRouter router) {
        setLayout(new BorderLayout());

        // --- HEADER ---
        JPanel header = new JPanel(new BorderLayout());

        JLabel title = new JLabel("IPOS-CA Dashboard", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 28));

        JLabel userInfo = new JLabel("", SwingConstants.RIGHT);
        userInfo.setFont(new Font("SansSerif", Font.PLAIN, 13));
        userInfo.setForeground(Color.GRAY);
        userInfo.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 16));

        header.add(title, BorderLayout.CENTER);
        header.add(userInfo, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // --- BUTTONS ---
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 12));

        JButton stockBtn    = new JButton("Stock");
        JButton salesBtn    = new JButton("Sales");
        JButton customerBtn = new JButton("Customers");
        JButton ordersBtn   = new JButton("Orders");
        JButton reportsBtn  = new JButton("Reports");
        JButton logoutBtn   = new JButton("Logout");

        buttons.add(stockBtn);
        buttons.add(salesBtn);
        buttons.add(customerBtn);
        buttons.add(ordersBtn);
        buttons.add(reportsBtn);
        buttons.add(logoutBtn);

        add(buttons, BorderLayout.CENTER);

        // --- ACTIONS ---
        stockBtn.addActionListener(e    -> router.goTo(MainFrame.SCREEN_STOCK));
        salesBtn.addActionListener(e    -> router.goTo(MainFrame.SCREEN_SALES));
        customerBtn.addActionListener(e -> router.goTo(MainFrame.SCREEN_CUSTOMERS));
        ordersBtn.addActionListener(e   -> router.goTo(MainFrame.SCREEN_ORDERS));
        reportsBtn.addActionListener(e  -> router.goTo(MainFrame.SCREEN_REPORTS));

        logoutBtn.addActionListener(e -> {
            Session.logout();
            router.goTo(MainFrame.SCREEN_LOGIN);
        });

        // --- ROLE-BASED VISIBILITY ---
        // called every time the panel is shown
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                User user = Session.getCurrentUser();
                if (user == null) return;

                userInfo.setText(user.getFullName() + "  |  " + user.getRole());

                // Reports only for MANAGER and ADMIN
                reportsBtn.setVisible(user.isManager() || user.isAdmin());

                // Customers and Orders not accessible by basic PHARMACIST
                customerBtn.setVisible(!user.isPharmacist());
                ordersBtn.setVisible(!user.isPharmacist());
            }
        });
    }
}