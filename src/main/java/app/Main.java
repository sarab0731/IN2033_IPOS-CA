package app;

import database.DatabaseSetup;
import database.MerchantDB;
import domain.Merchant;
import integration.CAApiServer;
import integration.PUCachePull;
import integration.ProductSyncScheduler;
import integration.SAOrderSyncScheduler;
import integration.SASync;
import ui.MainFrame;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {

        DatabaseSetup.initialise();
        CAApiServer.start();

        // Sync with SA: pull catalogue cache, verify merchant statuses, refresh financials
        SASync.syncWithSA();

        // Load the configured merchant into session after SA sync has refreshed the DB
        Merchant merchant = MerchantDB.getConfiguredMerchant();
        if (merchant != null) {
            Session.setMerchant(merchant);
            System.out.println("[Main] Merchant loaded: " + merchant.getMerchantId()
                    + " | status: " + merchant.getSaAccountStatus()
                    + " | credit: £" + merchant.getSaCreditLimit()
                    + " | balance: £" + merchant.getSaBalance());
        } else {
            System.out.println("[Main] No SA merchant configured.");
        }

        // Pull product cache from PU on startup (gets changes made while CA was offline)
        System.out.println("[Main] Starting CA, pulling cache from PU first...");
        int pulled = PUCachePull.pullCacheFromPU();
        System.out.println("[Main] Pulled " + pulled + " products from PU cache");
        
        // Start product sync scheduler (pushes CA products to PU every 30s)
        ProductSyncScheduler syncScheduler = new ProductSyncScheduler();
        syncScheduler.start();

        // Keep local restock orders aligned with SA approvals/dispatch/delivery.
        SAOrderSyncScheduler saOrderSyncScheduler = new SAOrderSyncScheduler();
        saOrderSyncScheduler.start();
        
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });

    }
}
