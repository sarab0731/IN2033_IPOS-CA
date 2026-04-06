package ui;

import app.Session;
import database.UserDB;
import domain.User;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.RoundRectangle2D;

public class LoginPanel extends JPanel {

    private final Color PAGE_BG = new Color(243, 243, 243);
    private final Color CARD_BG = new Color(248, 248, 248);
    private final Color CARD_BORDER = new Color(215, 215, 215);
    private final Color TEXT_DARK = new Color(63, 63, 63);
    private final Color TEXT_MID = new Color(117, 117, 117);
    private final Color FIELD_BORDER = new Color(205, 205, 205);
    private final Color BUTTON_BG = new Color(79, 79, 79);
    private final Color BUTTON_HOVER = new Color(65, 65, 65);
    private final Color ERROR_RED = new Color(180, 50, 50);

    private PlaceholderTextField usernameField;
    private JPasswordField passwordField;
    private JLabel msgLabel;

    public LoginPanel(ScreenRouter router) {
        setBackground(PAGE_BG);
        setLayout(new GridBagLayout());

        RoundedPanel card = new RoundedPanel(32, CARD_BG, CARD_BORDER);
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(420, 640));
        card.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("Sign in");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        titleLabel.setForeground(TEXT_DARK);

        JLabel subtitleLabel = new JLabel(
                "<html>Log in by entering your Username and<br>Password.</html>"
        );
        subtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        subtitleLabel.setForeground(TEXT_MID);

        JLabel usernameLabel = new JLabel("Username");
        usernameLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        usernameLabel.setForeground(TEXT_MID);

        JLabel passwordLabel = new JLabel("Password");
        passwordLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        passwordLabel.setForeground(TEXT_MID);

        usernameField = new PlaceholderTextField("Username", 20);
        usernameField.setFont(new Font("SansSerif", Font.BOLD, 16));
        usernameField.setForeground(TEXT_DARK);
        usernameField.setBorder(null);
        usernameField.setOpaque(false);

        passwordField = new JPasswordField(20);
        passwordField.setFont(new Font("SansSerif", Font.BOLD, 16));
        passwordField.setForeground(TEXT_DARK);
        passwordField.setBorder(null);
        passwordField.setOpaque(false);
        passwordField.setEchoChar('•');

        JPanel usernameWrapper = createInputWrapper("👤", usernameField, null, new Dimension(310, 46));
        JPanel passwordWrapper = createPasswordWrapper(passwordField, new Dimension(310, 46));

        JLabel forgotPassword = new JLabel("<html><u>Forgot password?</u></html>");
        forgotPassword.setFont(new Font("SansSerif", Font.PLAIN, 13));
        forgotPassword.setForeground(TEXT_DARK);
        forgotPassword.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JButton loginButton = createPrimaryButton("Log in", new Dimension(0, 50), 8, new Font("SansSerif", Font.BOLD, 18));

        msgLabel = new JLabel(" ");
        msgLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        msgLabel.setForeground(ERROR_RED);

        c.gridy = 0;
        c.insets = new Insets(55, 55, 12, 55);
        card.add(titleLabel, c);

        c.gridy++;
        c.insets = new Insets(0, 55, 28, 55);
        card.add(subtitleLabel, c);

        c.gridy++;
        c.insets = new Insets(0, 55, 8, 55);
        card.add(usernameLabel, c);

        c.gridy++;
        c.insets = new Insets(0, 55, 20, 55);
        card.add(usernameWrapper, c);

        c.gridy++;
        c.insets = new Insets(0, 55, 8, 55);
        card.add(passwordLabel, c);

        c.gridy++;
        c.insets = new Insets(0, 55, 8, 55);
        card.add(passwordWrapper, c);

        c.gridy++;
        c.insets = new Insets(0, 55, 28, 55);
        card.add(forgotPassword, c);

        c.gridy++;
        c.insets = new Insets(0, 55, 10, 55);
        card.add(loginButton, c);

        c.gridy++;
        c.insets = new Insets(12, 55, 0, 55);
        card.add(msgLabel, c);

        add(card);

        Runnable loginAction = () -> {
            String username = usernameField.getRealText().trim();
            String password = new String(passwordField.getPassword());

            if (username.isEmpty() || password.isEmpty()) {
                msgLabel.setText("Please enter your username and password.");
                return;
            }

            User user = UserDB.authenticate(username, password);

            if (user != null) {
                Session.login(user);
                msgLabel.setText(" ");
                router.goTo(MainFrame.SCREEN_DASHBOARD);
            } else {
                msgLabel.setText("Invalid username or password.");
                passwordField.setText("");
            }
        };

        loginButton.addActionListener(e -> loginAction.run());
        usernameField.addActionListener(e -> loginAction.run());
        passwordField.addActionListener(e -> loginAction.run());

        forgotPassword.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                startForgotPasswordFlow();
            }
        });

        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                clearFields();
            }
        });
    }

    public void clearFields() {
        if (usernameField != null) {
            usernameField.resetPlaceholder();
        }
        if (passwordField != null) {
            passwordField.setText("");
        }
        if (msgLabel != null) {
            msgLabel.setText(" ");
        }
    }

    private void startForgotPasswordFlow() {
        String targetUsername = askForTargetUsername();
        if (targetUsername == null) {
            return;
        }

        boolean adminApproved = showAdminApprovalDialog(targetUsername);
        if (!adminApproved) {
            return;
        }

        showResetPasswordCard(targetUsername);
    }

    private String askForTargetUsername() {
        final JDialog dialog = createStyledDialog("Forgot Password", 420, 290);
        final String[] resultHolder = {null};

        JPanel root = createDialogRoot();
        RoundedPanel card = createDialogCard(380, 240);
        root.add(card);

        GridBagConstraints c = baseCardConstraints();

        JLabel title = new JLabel("Forgot Password");
        title.setFont(new Font("SansSerif", Font.BOLD, 28));
        title.setForeground(TEXT_DARK);

        JLabel subtitle = new JLabel("<html>Enter the username that needs a password reset</html>");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 15));
        subtitle.setForeground(TEXT_MID);

        JTextField resetUserField = new JTextField();
        resetUserField.setFont(new Font("SansSerif", Font.BOLD, 15));
        resetUserField.setForeground(TEXT_DARK);
        resetUserField.setBorder(null);
        resetUserField.setOpaque(false);

        String currentUsername = usernameField.getRealText().trim();
        if (!currentUsername.isEmpty()) {
            resetUserField.setText(currentUsername);
        }

        JPanel fieldWrap = createInputWrapper("👤", resetUserField, null, new Dimension(270, 50));

        JLabel statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusLabel.setForeground(ERROR_RED);

        JPanel buttonRow = new JPanel(new GridLayout(1, 2, 12, 0));
        buttonRow.setOpaque(false);

        JButton cancelButton = createSecondaryButton("Cancel", new Dimension(0, 46), 8, new Font("SansSerif", Font.BOLD, 15));
        JButton okButton = createPrimaryButton("Continue", new Dimension(0, 46), 8, new Font("SansSerif", Font.BOLD, 15));

        buttonRow.add(cancelButton);
        buttonRow.add(okButton);

        c.gridy = 0;
        c.insets = new Insets(30, 36, 10, 36);
        card.add(title, c);

        c.gridy++;
        c.insets = new Insets(0, 36, 20, 36);
        card.add(subtitle, c);

        c.gridy++;
        c.insets = new Insets(0, 36, 8, 36);
        card.add(fieldWrap, c);

        c.gridy++;
        c.insets = new Insets(4, 36, 0, 36);
        card.add(statusLabel, c);

        c.gridy++;
        c.insets = new Insets(12, 36, 30, 36);
        card.add(buttonRow, c);

        Runnable submit = () -> {
            String usernameToReset = resetUserField.getText().trim();

            if (usernameToReset.isEmpty()) {
                statusLabel.setText("Please enter a username.");
                return;
            }

            if (!UserDB.usernameExists(usernameToReset)) {
                statusLabel.setText("That username does not exist.");
                return;
            }

            resultHolder[0] = usernameToReset;
            dialog.dispose();
        };

        okButton.addActionListener(e -> submit.run());
        resetUserField.addActionListener(e -> submit.run());
        cancelButton.addActionListener(e -> dialog.dispose());

        dialog.setContentPane(root);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);

        return resultHolder[0];
    }

    private boolean showAdminApprovalDialog(String targetUsername) {
        final JDialog dialog = createStyledDialog("Admin Approval Required", 460, 420);
        final boolean[] approved = {false};

        JPanel root = createDialogRoot();
        RoundedPanel card = createDialogCard(410, 360);
        root.add(card);

        GridBagConstraints c = baseCardConstraints();

        JLabel title = new JLabel("Admin Approval Required");
        title.setFont(new Font("SansSerif", Font.BOLD, 26));
        title.setForeground(TEXT_DARK);

        JLabel subtitle = new JLabel("<html>Reset for user: <b>" + targetUsername + "</b><br>Admin approval is required before changing the password.</html>");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 15));
        subtitle.setForeground(TEXT_MID);

        JLabel adminUserLabel = new JLabel("Admin username");
        adminUserLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        adminUserLabel.setForeground(TEXT_MID);

        JLabel adminPassLabel = new JLabel("Admin password");
        adminPassLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        adminPassLabel.setForeground(TEXT_MID);

        JTextField adminUserField = new JTextField();
        adminUserField.setFont(new Font("SansSerif", Font.BOLD, 15));
        adminUserField.setForeground(TEXT_DARK);
        adminUserField.setBorder(null);
        adminUserField.setOpaque(false);

        JPasswordField adminPassField = new JPasswordField();
        adminPassField.setFont(new Font("SansSerif", Font.BOLD, 15));
        adminPassField.setForeground(TEXT_DARK);
        adminPassField.setBorder(null);
        adminPassField.setOpaque(false);
        adminPassField.setEchoChar('•');

        JPanel adminUserWrap = createInputWrapper("👤", adminUserField, null, new Dimension(290, 50));
        JPanel adminPassWrap = createPasswordWrapper(adminPassField, new Dimension(290, 50));

        JLabel statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusLabel.setForeground(ERROR_RED);

        JPanel buttonRow = new JPanel(new GridLayout(1, 2, 12, 0));
        buttonRow.setOpaque(false);

        JButton cancelButton = createSecondaryButton("Cancel", new Dimension(0, 46), 8, new Font("SansSerif", Font.BOLD, 15));
        JButton approveButton = createPrimaryButton("Approve", new Dimension(0, 46), 8, new Font("SansSerif", Font.BOLD, 15));

        buttonRow.add(cancelButton);
        buttonRow.add(approveButton);

        c.gridy = 0;
        c.insets = new Insets(28, 36, 10, 36);
        card.add(title, c);

        c.gridy++;
        c.insets = new Insets(0, 36, 18, 36);
        card.add(subtitle, c);

        c.gridy++;
        c.insets = new Insets(0, 36, 8, 36);
        card.add(adminUserLabel, c);

        c.gridy++;
        c.insets = new Insets(0, 36, 14, 36);
        card.add(adminUserWrap, c);

        c.gridy++;
        c.insets = new Insets(0, 36, 8, 36);
        card.add(adminPassLabel, c);

        c.gridy++;
        c.insets = new Insets(0, 36, 10, 36);
        card.add(adminPassWrap, c);

        c.gridy++;
        c.insets = new Insets(4, 36, 0, 36);
        card.add(statusLabel, c);

        c.gridy++;
        c.insets = new Insets(14, 36, 28, 36);
        card.add(buttonRow, c);

        Runnable submit = () -> {
            String adminUsername = adminUserField.getText().trim();
            String adminPassword = new String(adminPassField.getPassword());

            if (adminUsername.isEmpty() || adminPassword.isEmpty()) {
                statusLabel.setText("Please enter the admin username and password.");
                return;
            }

            boolean ok = UserDB.authenticateAdmin(adminUsername, adminPassword);
            if (!ok) {
                statusLabel.setText("Admin authentication failed.");
                adminPassField.setText("");
                return;
            }

            approved[0] = true;
            dialog.dispose();
        };

        approveButton.addActionListener(e -> submit.run());
        adminUserField.addActionListener(e -> submit.run());
        adminPassField.addActionListener(e -> submit.run());
        cancelButton.addActionListener(e -> dialog.dispose());

        dialog.setContentPane(root);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);

        return approved[0];
    }

    private void showResetPasswordCard(String usernameToReset) {
        JDialog dialog = createStyledDialog("Reset Password", 470, 730);

        JPanel root = createDialogRoot();
        RoundedPanel card = createDialogCard(430, 680);
        root.add(card);

        GridBagConstraints c = baseCardConstraints();

        JLabel title = new JLabel("New password");
        title.setFont(new Font("SansSerif", Font.BOLD, 32));
        title.setForeground(TEXT_DARK);

        JLabel subtitle = new JLabel(
                "<html>Please set your new password, it must have at<br>least 8 characters.</html>"
        );
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 16));
        subtitle.setForeground(TEXT_MID);

        JLabel newPasswordLabel = new JLabel("New password");
        newPasswordLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        newPasswordLabel.setForeground(TEXT_MID);

        JLabel confirmPasswordLabel = new JLabel("Confirm password");
        confirmPasswordLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        confirmPasswordLabel.setForeground(TEXT_MID);

        JPasswordField newPasswordField = new JPasswordField(20);
        newPasswordField.setFont(new Font("SansSerif", Font.BOLD, 17));
        newPasswordField.setForeground(TEXT_DARK);
        newPasswordField.setBorder(null);
        newPasswordField.setOpaque(false);
        newPasswordField.setEchoChar('•');

        JPasswordField confirmPasswordField = new JPasswordField(20);
        confirmPasswordField.setFont(new Font("SansSerif", Font.BOLD, 17));
        confirmPasswordField.setForeground(TEXT_DARK);
        confirmPasswordField.setBorder(null);
        confirmPasswordField.setOpaque(false);
        confirmPasswordField.setEchoChar('•');

        JPanel newPassWrap = createPasswordWrapper(newPasswordField, new Dimension(300, 50));
        JPanel confirmPassWrap = createPasswordWrapper(confirmPasswordField, new Dimension(300, 50));

        JLabel statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusLabel.setForeground(ERROR_RED);

        JButton resetButton = createPrimaryButton(
                "Reset Password",
                new Dimension(300, 54),
                8,
                new Font("SansSerif", Font.BOLD, 17)
        );

        JLabel backLabel = new JLabel("<html><u>Go back to Sign In.</u></html>", SwingConstants.CENTER);
        backLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        backLabel.setForeground(TEXT_DARK);
        backLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        c.gridy = 0;
        c.insets = new Insets(58, 58, 14, 58);
        card.add(title, c);

        c.gridy++;
        c.insets = new Insets(0, 58, 36, 58);
        card.add(subtitle, c);

        c.gridy++;
        c.insets = new Insets(0, 58, 8, 58);
        card.add(newPasswordLabel, c);

        c.gridy++;
        c.insets = new Insets(0, 58, 18, 58);
        card.add(newPassWrap, c);

        c.gridy++;
        c.insets = new Insets(0, 58, 8, 58);
        card.add(confirmPasswordLabel, c);

        c.gridy++;
        c.insets = new Insets(0, 58, 18, 58);
        card.add(confirmPassWrap, c);

        c.gridy++;
        c.insets = new Insets(0, 58, 10, 58);
        card.add(resetButton, c);

        c.gridy++;
        c.insets = new Insets(6, 58, 0, 58);
        card.add(statusLabel, c);

        c.gridy++;
        c.weighty = 1.0;
        c.anchor = GridBagConstraints.SOUTH;
        c.insets = new Insets(0, 58, 48, 58);
        card.add(backLabel, c);

        Runnable submitReset = () -> {
            String newPassword = new String(newPasswordField.getPassword());
            String confirmPassword = new String(confirmPasswordField.getPassword());

            if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
                statusLabel.setText("Please complete both password fields.");
                return;
            }

            if (newPassword.length() < 8) {
                statusLabel.setText("Password must be at least 8 characters.");
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                statusLabel.setText("Passwords do not match.");
                return;
            }

            boolean updated = UserDB.resetPasswordByUsername(usernameToReset, newPassword);

            if (updated) {
                JOptionPane.showMessageDialog(
                        dialog,
                        "Password reset successful.",
                        "Reset Password",
                        JOptionPane.INFORMATION_MESSAGE
                );

                if (usernameField != null) {
                    usernameField.setText(usernameToReset);
                    usernameField.setForeground(TEXT_DARK);
                }
                if (passwordField != null) {
                    passwordField.setText("");
                }
                if (msgLabel != null) {
                    msgLabel.setText("Password reset successful. Please sign in.");
                }

                dialog.dispose();
            } else {
                statusLabel.setText("Could not reset password.");
            }
        };

        resetButton.addActionListener(e -> submitReset.run());
        newPasswordField.addActionListener(e -> submitReset.run());
        confirmPasswordField.addActionListener(e -> submitReset.run());

        backLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                dialog.dispose();
            }
        });

        dialog.setContentPane(root);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private JDialog createStyledDialog(String title, int width, int height) {
        JDialog dialog = new JDialog(
                SwingUtilities.getWindowAncestor(this),
                title,
                Dialog.ModalityType.APPLICATION_MODAL
        );
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setSize(width, height);
        dialog.setResizable(false);
        return dialog;
    }

    private JPanel createDialogRoot() {
        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(PAGE_BG);
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        return root;
    }

    private RoundedPanel createDialogCard(int width, int height) {
        RoundedPanel card = new RoundedPanel(34, CARD_BG, CARD_BORDER);
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(width, height));
        card.setLayout(new GridBagLayout());
        return card;
    }

    private GridBagConstraints baseCardConstraints() {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        return c;
    }

    private JButton createPrimaryButton(String text, Dimension size, int radius, Font font) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(true);
        button.setOpaque(true);
        button.setBackground(BUTTON_BG);
        button.setForeground(Color.WHITE);
        button.setFont(font);
        button.setPreferredSize(size);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(new RoundedBorder(radius, BUTTON_BG));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(BUTTON_HOVER);
                button.setBorder(new RoundedBorder(radius, BUTTON_HOVER));
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(BUTTON_BG);
                button.setBorder(new RoundedBorder(radius, BUTTON_BG));
            }
        });

        return button;
    }

    private JButton createSecondaryButton(String text, Dimension size, int radius, Font font) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setBorderPainted(true);
        button.setContentAreaFilled(true);
        button.setOpaque(true);
        button.setBackground(Color.WHITE);
        button.setForeground(TEXT_DARK);
        button.setFont(font);
        button.setPreferredSize(size);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(new RoundedLineBorder(FIELD_BORDER, 1, radius));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(245, 245, 245));
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(Color.WHITE);
            }
        });

        return button;
    }

    private JPanel createInputWrapper(String iconText, JTextField field, JButton trailingButton, Dimension size) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(Color.WHITE);
        wrapper.setBorder(new RoundedLineBorder(FIELD_BORDER, 2, 10));
        wrapper.setPreferredSize(size);
        wrapper.setMaximumSize(size);

        JLabel iconLabel = new JLabel(iconText);
        iconLabel.setFont(new Font("SansSerif", Font.PLAIN, 18));
        iconLabel.setForeground(new Color(145, 145, 145));
        iconLabel.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 10));

        wrapper.add(iconLabel, BorderLayout.WEST);
        wrapper.add(field, BorderLayout.CENTER);

        if (trailingButton != null) {
            wrapper.add(trailingButton, BorderLayout.EAST);
        }

        return wrapper;
    }

    private JPanel createPasswordWrapper(JPasswordField passwordField, Dimension size) {
        JButton toggleButton = new JButton("👁");
        toggleButton.setFocusPainted(false);
        toggleButton.setBorderPainted(false);
        toggleButton.setContentAreaFilled(false);
        toggleButton.setOpaque(false);
        toggleButton.setForeground(new Color(150, 150, 150));
        toggleButton.setFont(new Font("SansSerif", Font.PLAIN, 17));
        toggleButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        toggleButton.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 14));

        final boolean[] visible = {false};

        toggleButton.addActionListener(e -> {
            visible[0] = !visible[0];
            if (visible[0]) {
                passwordField.setEchoChar((char) 0);
            } else {
                passwordField.setEchoChar('•');
            }
        });

        return createInputWrapper("🔒", passwordField, toggleButton, size);
    }

    private static class RoundedPanel extends JPanel {
        private final int radius;
        private final Color fillColor;
        private final Color borderColor;

        public RoundedPanel(int radius, Color fillColor, Color borderColor) {
            this.radius = radius;
            this.fillColor = fillColor;
            this.borderColor = borderColor;
            setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth() - 1;
            int h = getHeight() - 1;

            g2.setColor(fillColor);
            g2.fillRoundRect(0, 0, w, h, radius, radius);

            g2.setColor(borderColor);
            g2.drawRoundRect(0, 0, w, h, radius, radius);

            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class RoundedLineBorder extends AbstractBorder {
        private final Color color;
        private final int thickness;
        private final int radius;

        public RoundedLineBorder(Color color, int thickness, int radius) {
            this.color = color;
            this.thickness = thickness;
            this.radius = radius;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(thickness));
            g2.draw(new RoundRectangle2D.Double(
                    x + thickness / 2.0,
                    y + thickness / 2.0,
                    width - thickness,
                    height - thickness,
                    radius,
                    radius
            ));
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(8, 8, 8, 8);
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.left = 8;
            insets.right = 8;
            insets.top = 8;
            insets.bottom = 8;
            return insets;
        }
    }

    private static class RoundedBorder extends AbstractBorder {
        private final int radius;
        private final Color color;

        public RoundedBorder(int radius, Color color) {
            this.radius = radius;
            this.color = color;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
            g2.dispose();
        }
    }

    private static class PlaceholderTextField extends JTextField {
        private final String placeholder;
        private boolean showingPlaceholder = true;

        public PlaceholderTextField(String placeholder, int columns) {
            super(columns);
            this.placeholder = placeholder;
            showPlaceholder();

            addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    if (showingPlaceholder) {
                        setText("");
                        setForeground(new Color(66, 66, 66));
                        showingPlaceholder = false;
                    }
                }

                @Override
                public void focusLost(FocusEvent e) {
                    if (getText().trim().isEmpty()) {
                        showPlaceholder();
                    }
                }
            });
        }

        private void showPlaceholder() {
            setText(placeholder);
            setForeground(new Color(90, 90, 90));
            showingPlaceholder = true;
        }

        public void resetPlaceholder() {
            showPlaceholder();
        }

        public String getRealText() {
            return showingPlaceholder ? "" : getText();
        }
    }
}