package integration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PaymentProcessingImplTest {

    private static PaymentProcessingImpl service;

    @BeforeAll
    static void setUp() {
        service = new PaymentProcessingImpl();
    }

    @Test
    void testNullCardDetails() {
        String result = service.requestPayment("MCH-001", "ORD-001", "Alice Smith", "1 High St", null, 50.00);
        assertNotNull(result);
        assertTrue(result.startsWith("ERROR"));
    }

    @Test
    void testCardDetailsArrayTooShort() {
        String[] twoElements = {"4111111111111111", "12/26"};
        String result = service.requestPayment("MCH-001", "ORD-001", "Alice Smith", "1 High St", twoElements, 50.00);
        assertNotNull(result);
        assertTrue(result.startsWith("ERROR"));
    }

    @Test
    void testEmptyCardDetailsArray() {
        String result = service.requestPayment("MCH-001", "ORD-001", "Alice Smith", "1 High St", new String[]{}, 50.00);
        assertNotNull(result);
        assertTrue(result.startsWith("ERROR"));
    }

    @Test
    void testZeroAmount() {
        String[] card = {"4111111111111111", "12/26", "123"};
        String result = service.requestPayment("MCH-001", "ORD-001", "Alice Smith", "1 High St", card, 0.00);
        assertNotNull(result);
        assertTrue(result.startsWith("ERROR"));
    }

    @Test
    void testNegativeAmount() {
        String[] card = {"4111111111111111", "12/26", "123"};
        String result = service.requestPayment("MCH-001", "ORD-001", "Alice Smith", "1 High St", card, -10.00);
        assertNotNull(result);
        assertTrue(result.startsWith("ERROR"));
    }

    @Test
    void testNoExceptionThrown() {
        String[] card = {"4111111111111111", "12/26", "123"};
        assertDoesNotThrow(() -> service.requestPayment("MCH-001", "ORD-001", "Alice Smith", "1 High St", card, 99.99));
    }
}
