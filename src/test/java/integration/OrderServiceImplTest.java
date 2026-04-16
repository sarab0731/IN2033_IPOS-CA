package integration;

import domain.DiscountPlan;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class OrderServiceImplTest {

    private static OrderServiceImpl service;

    @BeforeAll
    static void setUp() {
        service = new OrderServiceImpl();
    }

    @Test
    void testMalformedJsonReturnsError() {
        String result = service.placeRestockOrder("MCH-001", "NOT_VALID_JSON{{{{");
        assertNotNull(result);
        assertTrue(result.startsWith("ERROR"));
    }

    @Test
    void testNullOrderDetailsReturnsError() {
        String result = service.placeRestockOrder("MCH-001", null);
        assertNotNull(result);
        assertTrue(result.startsWith("ERROR"));
    }

    @Test
    void testEmptyOrderDetailsReturnsError() {
        String result = service.placeRestockOrder("MCH-001", "");
        assertNotNull(result);
        assertTrue(result.startsWith("ERROR"));
    }

    @Test
    void testNoExceptionThrown() {
        assertDoesNotThrow(() -> service.placeRestockOrder("MCH-001", null));
    }

    @Test
    void testViewDiscountPlanNullReturnsFalse() {
        assertFalse(service.viewDiscountPlan("MCH-001", null));
    }

    @Test
    void testViewDiscountPlanWithPlanReturnsTrue() {
        DiscountPlan plan = new DiscountPlan(1, "Standard", "FIXED", 10.0, "");
        assertTrue(service.viewDiscountPlan("MCH-001", plan));
    }
}
