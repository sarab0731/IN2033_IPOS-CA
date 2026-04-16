package domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class UserTest {

    private User admin;
    private User manager;
    private User pharmacist;

    @BeforeEach
    void setUp() {
        admin = new User(1, "admin_user", "Alice Admin", "ADMIN");
        manager = new User(2, "mgr_user", "Bob Manager", "MANAGER");
        pharmacist = new User(3, "pharm_user", "Carol Pharm", "PHARMACIST");
    }

    @Test
    void testGetters() {
        assertEquals(1, admin.getUserId());
        assertEquals("admin_user", admin.getUsername());
        assertEquals("Alice Admin", admin.getFullName());
        assertEquals("ADMIN", admin.getRole());
    }

    @Test
    void testAdminRole() {
        assertTrue(admin.isAdmin());
        assertFalse(manager.isAdmin());
        assertFalse(pharmacist.isAdmin());
    }

    @Test
    void testManagerRole() {
        assertFalse(admin.isManager());
        assertTrue(manager.isManager());
        assertFalse(pharmacist.isManager());
    }

    @Test
    void testPharmacistRole() {
        assertFalse(admin.isPharmacist());
        assertFalse(manager.isPharmacist());
        assertTrue(pharmacist.isPharmacist());
    }
}
