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
    private final Color CARD_BORDER = new Color(210, 210, 210);
    private final Color TEXT_DARK = new Color(66, 66, 66);
    private final Color TEXT_MID = new Color(110, 110, 110);
    private final Color FIELD_BORDER = new Color(200, 200, 200);
    private final Color BUTTON_BG = new Color(74, 74, 74);
    private final Color BUTTON_HOVER = new Color(60, 60, 60);
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

        JPanel usernameWrapper = createInputWrapper("👤", usernameField, null);
        JPanel passwordWrapper = createPasswordWrapper(passwordField);

        JLabel forgotPassword = new JLabel("<html><u>Forgot password?</u></html>");
        forgotPassword.setFont(new Font("SansSerif", Font.PLAIN, 13));
        forgotPassword.setForeground(TEXT_DARK);
        forgotPassword.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JButton loginButton = new JButton("Log in");
        loginButton.setFocusPainted(false);
        loginButton.setBorderPainted(false);
        loginButton.setContentAreaFilled(true);
        loginButton.setOpaque(true);
        loginButton.setBackground(BUTTON_BG);
        loginButton.setForeground(Color.WHITE);
        loginButton.setFont(new Font("SansSerif", Font.BOLD, 18));
        loginButton.setPreferredSize(new Dimension(0, 50));
        loginButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        loginButton.setBorder(new RoundedBorder(6, BUTTON_BG));

        loginButton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                loginButton.setBackground(BUTTON_HOVER);
                loginButton.setBorder(new RoundedBorder(6, BUTTON_HOVER));
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                loginButton.setBackground(BUTTON_BG);
                loginButton.setBorder(new RoundedBorder(6, BUTTON_BG));
            }
        });

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
                JOptionPane.showMessageDialog(
                        LoginPanel.this,
                        "Password reset is not implemented yet.",
                        "Forgot Password",
                        JOptionPane.INFORMATION_MESSAGE
                );
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

    private JPanel createInputWrapper(String iconText, JTextField field, JButton trailingButton) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(Color.WHITE);
        wrapper.setBorder(new RoundedLineBorder(FIELD_BORDER, 2, 6));
        wrapper.setPreferredSize(new Dimension(310, 46));

        JLabel iconLabel = new JLabel(iconText);
        iconLabel.setFont(new Font("SansSerif", Font.PLAIN, 18));
        iconLabel.setForeground(new Color(120, 120, 120));
        iconLabel.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 10));

        wrapper.add(iconLabel, BorderLayout.WEST);
        wrapper.add(field, BorderLayout.CENTER);

        if (trailingButton != null) {
            wrapper.add(trailingButton, BorderLayout.EAST);
        }

        return wrapper;
    }

    private JPanel createPasswordWrapper(JPasswordField passwordField) {
        JButton toggleButton = new JButton("👁");
        toggleButton.setFocusPainted(false);
        toggleButton.setBorderPainted(false);
        toggleButton.setContentAreaFilled(false);
        toggleButton.setOpaque(false);
        toggleButton.setForeground(new Color(140, 140, 140));
        toggleButton.setFont(new Font("SansSerif", Font.PLAIN, 16));
        toggleButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        toggleButton.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 12));

        final boolean[] visible = {false};

        toggleButton.addActionListener(e -> {
            visible[0] = !visible[0];
            if (visible[0]) {
                passwordField.setEchoChar((char) 0);
            } else {
                passwordField.setEchoChar('•');
            }
        });

        JPanel wrapper = createInputWrapper("🔒", passwordField, toggleButton);
        wrapper.setPreferredSize(new Dimension(310, 46));
        return wrapper;
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