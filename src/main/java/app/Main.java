package app;

import database.DatabaseSetup;
import integration.CAApiServer;
import ui.MainFrame;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {

        DatabaseSetup.initialise();
        CAApiServer.start();
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });

    }
}