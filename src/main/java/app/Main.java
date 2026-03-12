package app;

import database.DatabaseSetup;
import database.UserTest;
import ui.MainFrame;
import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });

        DatabaseSetup.initialise();

        UserTest.insertUser("admin", "admin123", "Admin User", "ADMIN");
        UserTest.insertUser("pharm1", "pass123", "Alice Smith", "PHARMACIST");

        UserTest.printUsers();
    }
}
