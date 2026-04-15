package app;

import domain.Merchant;
import domain.User;

public class Session {

    private static User     currentUser;
    private static Merchant merchant;     // the single SA merchant account for this CA

    public static void login(User user) {
        currentUser = user;
    }

    public static void logout() {
        currentUser = null;
        // merchant stays — it is pharmacy-level config, not user-level
    }

    // ── Merchant (SA account) ─────────────────────────────────────────────────

    public static void setMerchant(Merchant m)  { merchant = m; }
    public static Merchant getMerchant() {
        if (merchant == null) merchant = database.MerchantDB.getConfiguredMerchant();
        return merchant;
    }
    public static String   getMerchantId()       { return getMerchant() != null ? getMerchant().getMerchantId() : null; }
    public static boolean  hasMerchant()         { return getMerchant() != null; }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    public static boolean isAdmin() {
        return currentUser != null && currentUser.isAdmin();
    }

    public static boolean isManager() {
        return currentUser != null && currentUser.isManager();
    }

    public static boolean isPharmacist() {
        return currentUser != null && currentUser.isPharmacist();
    }

    public static boolean isManagerOrAdmin() {
        return isManager() || isAdmin();
    }

    public static boolean hasRole(String role) {
        return currentUser != null && currentUser.getRole().equals(role);
    }

    public static int getUserId() {
        return currentUser != null ? currentUser.getUserId() : -1;
    }
}