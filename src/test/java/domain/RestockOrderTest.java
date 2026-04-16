package domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class RestockOrderTest {

    private RestockOrder order;

    @BeforeEach
    void setUp() {
        order = new RestockOrder(1, "RST-2024-001", "MCH-001", "PENDING", 350.75, "2024-01-15 10:00:00");
    }

    @Test
    void testGetters() {
        assertEquals(1, order.getRestockOrderId());
        assertEquals("RST-2024-001", order.getOrderNumber());
        assertEquals("MCH-001", order.getMerchantId());
        assertEquals("PENDING", order.getStatus());
        assertEquals(350.75, order.getTotalValue(), 0.001);
        assertEquals("2024-01-15 10:00:00", order.getCreatedAt());
    }

    @Test
    void testToString() {
        String result = order.toString();
        assertTrue(result.contains("RST-2024-001"));
        assertTrue(result.contains("PENDING"));
        assertTrue(result.contains("350.75"));
    }
}
