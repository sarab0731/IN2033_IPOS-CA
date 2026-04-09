package app;

import database.DatabaseSetup;
import integration.CAApiServer;
import integration.PUCachePull;
import integration.ProductSyncScheduler;
import ui.MainFrame;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {

        DatabaseSetup.initialise();
        CAApiServer.start();
        
        // Pull product cache from PU on startup (gets changes made while CA was offline)
        System.out.println("[Main] Starting CA, pulling cache from PU first...");
        int pulled = PUCachePull.pullCacheFromPU();
        System.out.println("[Main] Pulled " + pulled + " products from PU cache");
        
        // Start product sync scheduler (pushes CA products to PU every 30s)
        ProductSyncScheduler syncScheduler = new ProductSyncScheduler();
        syncScheduler.start();
        
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });

    }
}
