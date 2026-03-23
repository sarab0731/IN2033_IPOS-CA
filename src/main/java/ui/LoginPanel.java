package ui;

import app.Session;
import database.UserDB;
import domain.User;

import javax.swing.*;
import java.awt.*;

public class LoginPanel extends JPanel {

    public LoginPanel(ScreenRouter router) {
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(10, 10, 10, 10);
        c.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("IPOS-CA", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 32));

        JLabel subtitle = new JLabel("Pharmacy Client Application", SwingConstants.CENTER);
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 14));
        subtitle.setForeground(Color.GRAY);

        JTextField usernameField = new JTextField(20);
        JPasswordField passwordField = new JPasswordField(20);

        JButton loginBtn = new JButton("Login");
        loginBtn.setFont(new Font("SansSerif", Font.BOLD, 14));

        JLabel msgLabel = new JLabel(" ", SwingConstants.CENTER);
        msgLabel.setForeground(Color.RED);

        c.gridx = 0; c.gridy = 0; c.gridwidth = 2;
        add(title, c);

        c.gridy = 1;
        add(subtitle, c);

        c.gridy = 2; c.gridwidth = 1; c.gridx = 0;
        add(new JLabel("Username:"), c);
        c.gridx = 1;
        add(usernameField, c);

        c.gridy = 3; c.gridx = 0;
        add(new JLabel("Password:"), c);
        c.gridx = 1;
        add(passwordField, c);

        c.gridy = 4; c.gridx = 0; c.gridwidth = 2;
        add(loginBtn, c);

        c.gridy = 5;
        add(msgLabel, c);

        loginBtn.addActionListener(e -> {
            String username = usernameField.getText().trim();
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
        });

        // allow login on Enter key
        passwordField.addActionListener(e -> loginBtn.doClick());
    }
}