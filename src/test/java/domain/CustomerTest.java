package domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CustomerTest {

    private Customer active;
    private Customer suspended;
    private Customer inDefault;

    @BeforeEach
    void setUp() {
        active    = new Customer(1, "Alice Smith", "alice@email.com", "07700000001", "1 High St", 1000.00, 250.00, "ACTIVE", 0);
        suspended = new Customer(2, "Bob Jones",   "bob@email.com",   "07700000002", "2 Low St",  500.00,  500.00, "SUSPENDED", 1);
        inDefault = new Customer(3, "Carol White", "carol@email.com", "07700000003", "3 Mill Rd", 200.00,  300.00, "IN_DEFAULT", 0);
    }

    @Test
    void testGetters() {
        assertEquals(1, active.getCustomerId());
        assertEquals("Alice Smith", active.getFullName());
        assertEquals("alice@email.com", active.getEmail());
        assertEquals("07700000001", active.getPhone());
        assertEquals("1 High St", active.getAddress());
        assertEquals(1000.00, active.getCreditLimit(), 0.001);
        assertEquals(250.00, active.getCurrentBalance(), 0.001);
        assertEquals("ACTIVE", active.getAccountStatus());
        assertEquals(0, active.getDiscountPlanId());
    }

    @Test
    void testActiveStatus() {
        assertTrue(active.isActive());
        assertFalse(suspended.isActive());
        assertFalse(inDefault.isActive());
    }

    @Test
    void testSuspendedStatus() {
        assertFalse(active.isSuspended());
        assertTrue(suspended.isSuspended());
        assertFalse(inDefault.isSuspended());
    }

    @Test
    void testInDefaultStatus() {
        assertFalse(active.isInDefault());
        assertFalse(suspended.isInDefault());
        assertTrue(inDefault.isInDefault());
    }

    @Test
    void testAvailableCredit() {
        assertEquals(750.00, active.getAvailableCredit(), 0.001);
    }

    @Test
    void testAvailableCreditWhenZero() {
        assertEquals(0.00, suspended.getAvailableCredit(), 0.001);
    }

    @Test
    void testAvailableCreditWhenNegative() {
        assertEquals(-100.00, inDefault.getAvailableCredit(), 0.001);
    }

    @Test
    void testToString() {
        assertEquals("Alice Smith", active.toString());
    }
}
