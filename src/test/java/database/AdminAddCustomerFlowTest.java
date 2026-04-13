package database;

import app.Session;
import domain.Customer;
import domain.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AdminAddCustomerFlowTest {

    @BeforeAll
    static void setUpDatabase() {
        DatabaseSetup.initialise();
    }

    @AfterEach
    void clearSession() {
        Session.logout();
    }

    @Test
    void adminRolePassesManagerOrAdminGate() {
        Session.login(new User(9999, "qa_admin", "QA Admin", "ADMIN"));
        assertTrue(Session.isAdmin(), "ADMIN user should be recognized as admin");
        assertTrue(Session.isManagerOrAdmin(), "ADMIN user should pass manager/admin gate");
    }

    @Test
    void adminCanAddCustomerRecord() {
        Session.login(new User(9999, "qa_admin", "QA Admin", "ADMIN"));
        assertTrue(Session.isManagerOrAdmin(), "Precondition failed: admin gate not satisfied");

        String accountNumber = "AUTO-ADM-" + System.currentTimeMillis();
        Customer customer = new Customer(
                0,
                accountNumber,
                "Admin Flow Test",
                "admin-flow@test.local",
                "07000000000",
                "1 Admin Street",
                1500.00,
                0.00,
                "ACTIVE",
                0
        );

        Customer inserted = null;
        try {
            boolean added = CustomerDB.addCustomer(customer);
            assertTrue(added, "CustomerDB.addCustomer should succeed for valid data");

            inserted = CustomerDB.getByAccountNumber(accountNumber);
            assertNotNull(inserted, "Inserted customer should be retrievable by account number");
            assertEquals("Admin Flow Test", inserted.getFullName());
            assertEquals("ACTIVE", inserted.getAccountStatus());
        } finally {
            if (inserted == null) {
                inserted = CustomerDB.getByAccountNumber(accountNumber);
            }
            if (inserted != null) {
                CustomerDB.deleteCustomer(inserted.getCustomerId());
            }
        }
    }
}
