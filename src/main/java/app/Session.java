package app;

import domain.User;

public class Session {

    private static User currentUser;

    public static void login(User user) {
        currentUser = user;
    }

    public static void logout() {
        currentUser = null;
    }

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