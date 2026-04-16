package app;

import domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SessionTest {

    @BeforeEach
    void clearSession() {
        Session.logout();
    }

    @Test
    void testNotLoggedInByDefault() {
        assertFalse(Session.isLoggedIn());
    }

    @Test
    void testLoggedInAfterLogin() {
        Session.login(new User(1, "alice", "Alice Admin", "ADMIN"));
        assertTrue(Session.isLoggedIn());
    }

    @Test
    void testLoggedOutAfterLogout() {
        Session.login(new User(1, "alice", "Alice Admin", "ADMIN"));
        Session.logout();
        assertFalse(Session.isLoggedIn());
    }

    @Test
    void testCurrentUserIsNullAfterLogout() {
        Session.login(new User(1, "alice", "Alice Admin", "ADMIN"));
        Session.logout();
        assertNull(Session.getCurrentUser());
    }

    @Test
    void testGetCurrentUser() {
        User user = new User(2, "bob", "Bob Manager", "MANAGER");
        Session.login(user);
        assertEquals(user, Session.getCurrentUser());
    }

    @Test
    void testGetUserIdWhenNotLoggedIn() {
        assertEquals(-1, Session.getUserId());
    }

    @Test
    void testGetUserIdWhenLoggedIn() {
        Session.login(new User(42, "carol", "Carol Pharm", "PHARMACIST"));
        assertEquals(42, Session.getUserId());
    }

    @Test
    void testAdminRoleCheck() {
        Session.login(new User(1, "alice", "Alice Admin", "ADMIN"));
        assertTrue(Session.isAdmin());
        assertFalse(Session.isManager());
        assertFalse(Session.isPharmacist());
    }

    @Test
    void testManagerRoleCheck() {
        Session.login(new User(2, "bob", "Bob Manager", "MANAGER"));
        assertFalse(Session.isAdmin());
        assertTrue(Session.isManager());
        assertFalse(Session.isPharmacist());
    }

    @Test
    void testPharmacistRoleCheck() {
        Session.login(new User(3, "carol", "Carol Pharm", "PHARMACIST"));
        assertFalse(Session.isAdmin());
        assertFalse(Session.isManager());
        assertTrue(Session.isPharmacist());
    }

    @Test
    void testIsManagerOrAdmin() {
        Session.login(new User(1, "alice", "Alice Admin", "ADMIN"));
        assertTrue(Session.isManagerOrAdmin());

        Session.logout();
        Session.login(new User(2, "bob", "Bob Manager", "MANAGER"));
        assertTrue(Session.isManagerOrAdmin());

        Session.logout();
        Session.login(new User(3, "carol", "Carol Pharm", "PHARMACIST"));
        assertFalse(Session.isManagerOrAdmin());
    }

    @Test
    void testRoleChecksWhenNotLoggedIn() {
        assertFalse(Session.isAdmin());
        assertFalse(Session.isManager());
        assertFalse(Session.isPharmacist());
        assertFalse(Session.isManagerOrAdmin());
    }

    @Test
    void testHasRole() {
        Session.login(new User(1, "alice", "Alice Admin", "ADMIN"));
        assertTrue(Session.hasRole("ADMIN"));
        assertFalse(Session.hasRole("MANAGER"));
    }

    @Test
    void testHasRoleWhenNotLoggedIn() {
        assertFalse(Session.hasRole("ADMIN"));
    }
}
