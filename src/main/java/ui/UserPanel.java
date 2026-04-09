package ui;

import app.Session;
import database.UserDB;
import domain.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.List;

public class UserPanel extends JPanel implements ThemeManager.ThemeListener {

    private final ScreenRouter router;

    private JPanel contentPanel;
    private JPanel topControls;
    private JPanel bottomControls;
    private JPanel tableCard;

    private JTable table;
    private JScrollPane scrollPane;
    private DefaultTableModel tableModel;

    private JButton addUserBtn;
    private JButton changeRoleBtn;
    private JButton deleteUserBtn;
    private JButton refreshBtn;

    public UserPanel(ScreenRouter router) {
        this.router = router;

        setLayout(new BorderLayout());
        ThemeManager.register(this);

        AppShell shell = new AppShell(
                router,
                MainFrame.SCREEN_USERS,
                "User Management",
                "Create and manage staff accounts",
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
                new String[]{"ID", "Username", "Full Name", "Role", "Active"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        table = new JTable(tableModel);
        configureTable(table);

        scrollPane = new JScrollPane(table);
        styleScrollPane(scrollPane);

        tableCard.add(scrollPane, BorderLayout.CENTER);

        bottomControls = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        bottomControls.setOpaque(false);

        addUserBtn    = createBtn("Add User", true);
        changeRoleBtn = createBtn("Change Role", false);
        deleteUserBtn = createBtn("Delete User", false);
        refreshBtn    = createBtn("Refresh", false);

        bottomControls.add(addUserBtn);
        bottomControls.add(changeRoleBtn);
        bottomControls.add(deleteUserBtn);
        bottomControls.add(refreshBtn);

        contentPanel.add(tableCard, BorderLayout.CENTER);
        contentPanel.add(bottomControls, BorderLayout.SOUTH);

        return contentPanel;
    }

    private void wireActions() {
        refreshBtn.addActionListener(e -> loadTable());

        addUserBtn.addActionListener(e -> {
            if (!Session.isAdmin()) {
                JOptionPane.showMessageDialog(this, "Only Admins can create user accounts.");
                return;
            }
            showAddUserDialog();
        });

        changeRoleBtn.addActionListener(e -> {
            if (!Session.isAdmin()) {
                JOptionPane.showMessageDialog(this, "Only Admins can change roles.");
                return;
            }
            int row = table.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Please select a user.");
                return;
            }
            showChangeRoleDialog(row);
        });

        deleteUserBtn.addActionListener(e -> {
            if (!Session.isAdmin()) {
                JOptionPane.showMessageDialog(this, "Only Admins can delete user accounts.");
                return;
            }
            int row = table.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Please select a user.");
                return;
            }
            int userId = (int) tableModel.getValueAt(row, 0);
            if (userId == Session.getUserId()) {
                JOptionPane.showMessageDialog(this, "You cannot delete your own account.");
                return;
            }
            String name = (String) tableModel.getValueAt(row, 2);
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Delete account for: " + name + "?",
                    "Confirm Delete", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                UserDB.deactivateUser(userId);
                loadTable();
            }
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
        List<User> users = UserDB.getActiveUsers();
        for (User u : users) {
            tableModel.addRow(new Object[]{
                    u.getUserId(),
                    u.getUsername(),
                    u.getFullName(),
                    u.getRole(),
                    "Active"
            });
        }
    }

    private void showAddUserDialog() {
        JTextField usernameField = new JTextField();
        JTextField fullNameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        JPasswordField confirmField  = new JPasswordField();
        JComboBox<String> roleCombo  = new JComboBox<>(new String[]{"PHARMACIST", "MANAGER", "ADMIN"});

        Object[] fields = {
                "Username:",         usernameField,
                "Full Name:",        fullNameField,
                "Password:",         passwordField,
                "Confirm Password:", confirmField,
                "Role:",             roleCombo
        };

        int result = JOptionPane.showConfirmDialog(this, fields, "Add User Account", JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) return;

        String username  = usernameField.getText().trim();
        String fullName  = fullNameField.getText().trim();
        String password  = new String(passwordField.getPassword());
        String confirm   = new String(confirmField.getPassword());
        String role      = (String) roleCombo.getSelectedItem();

        if (username.isEmpty() || fullName.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All fields are required.");
            return;
        }
        if (!password.equals(confirm)) {
            JOptionPane.showMessageDialog(this, "Passwords do not match.");
            return;
        }
        if (password.length() < 6) {
            JOptionPane.showMessageDialog(this, "Password must be at least 6 characters.");
            return;
        }

        if (UserDB.createUser(username, password, fullName, role)) {
            loadTable();
            JOptionPane.showMessageDialog(this, "User account created successfully.");
        } else {
            JOptionPane.showMessageDialog(this, "Failed to create user. Username may already exist.");
        }
    }

    private void showChangeRoleDialog(int row) {
        int    userId      = (int) tableModel.getValueAt(row, 0);
        String currentRole = (String) tableModel.getValueAt(row, 3);
        String name        = (String) tableModel.getValueAt(row, 2);

        if (userId == Session.getUserId()) {
            JOptionPane.showMessageDialog(this, "You cannot change your own role.");
            return;
        }

        String[] roles = {"PHARMACIST", "MANAGER", "ADMIN"};
        String chosen = (String) JOptionPane.showInputDialog(
                this,
                "Change role for: " + name + "\nCurrent role: " + currentRole,
                "Change Role",
                JOptionPane.PLAIN_MESSAGE,
                null,
                roles,
                currentRole
        );

        if (chosen != null && !chosen.equals(currentRole)) {
            UserDB.updateRole(userId, chosen);
            loadTable();
            JOptionPane.showMessageDialog(this, "Role updated to " + chosen + ".");
        }
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
        header.setFont(new Font("SansSerif", Font.BOLD, 13));
        header.setBorder(BorderFactory.createEmptyBorder());

        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.setHorizontalAlignment(SwingConstants.LEFT);
        renderer.setBorder(new EmptyBorder(0, 10, 0, 10));
        table.setDefaultRenderer(Object.class, renderer);
    }

    private void styleScrollPane(JScrollPane sp) {
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setOpaque(true);
        sp.setBackground(ThemeManager.tableBackground());
        sp.getViewport().setBackground(ThemeManager.tableBackground());
        sp.getViewport().setBorder(null);
    }

    private void applyTableTheme(JTable table) {
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
            header.setOpaque(true);
        }

        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.setBackground(ThemeManager.tableBackground());
        renderer.setForeground(ThemeManager.textPrimary());
        renderer.setHorizontalAlignment(SwingConstants.LEFT);
        renderer.setBorder(new EmptyBorder(0, 10, 0, 10));
        table.setDefaultRenderer(Object.class, renderer);
    }

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

    @Override
    public void applyTheme() {
        setBackground(ThemeManager.appBackground());
        if (contentPanel   != null) contentPanel.setBackground(ThemeManager.appBackground());
        if (bottomControls != null) bottomControls.setBackground(ThemeManager.appBackground());
        if (tableCard      != null) tableCard.setBackground(ThemeManager.panelBackground());
        if (table          != null) applyTableTheme(table);
        if (scrollPane     != null) styleScrollPane(scrollPane);
        if (addUserBtn    != null) { addUserBtn.setBackground(ThemeManager.buttonDark()); addUserBtn.setForeground(ThemeManager.textLight()); }
        if (changeRoleBtn != null) { changeRoleBtn.setBackground(ThemeManager.buttonLight()); changeRoleBtn.setForeground(ThemeManager.textPrimary()); }
        if (deleteUserBtn != null) { deleteUserBtn.setBackground(ThemeManager.buttonLight()); deleteUserBtn.setForeground(ThemeManager.textPrimary()); }
        if (refreshBtn    != null) { refreshBtn.setBackground(ThemeManager.buttonLight()); refreshBtn.setForeground(ThemeManager.textPrimary()); }
        repaint();
        revalidate();
    }
}