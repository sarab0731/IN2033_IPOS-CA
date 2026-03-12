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

        // You can create new test items and test them here
        UserTest.insertUser("admin2", "admin1234", "Admin Userr", "ADMIN");
        UserTest.insertUser("pharm3", "pass1234", "Alice Smithh", "PHARMACIST");

        UserTest.printUsers();
    }
}
