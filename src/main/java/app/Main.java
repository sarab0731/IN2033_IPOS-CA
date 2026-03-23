package app;

import database.DatabaseSetup;
import ui.MainFrame;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {

        DatabaseSetup.initialise();
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}