package domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class RestockOrderItemTest {

    private RestockOrderItem item;

    @BeforeEach
    void setUp() {
        item = new RestockOrderItem(10, 1, "ITM-001", "Paracetamol 500mg", 5, 2.50, 12.50);
    }

    @Test
    void testGetters() {
        assertEquals(10, item.getRestockOrderItemId());
        assertEquals(1, item.getRestockOrderId());
        assertEquals("ITM-001", item.getItemId());
        assertEquals("Paracetamol 500mg", item.getDescription());
        assertEquals(5, item.getQuantity());
        assertEquals(2.50, item.getUnitCost(), 0.001);
        assertEquals(12.50, item.getLineTotal(), 0.001);
    }

    @Test
    void testLineTotalMatchesQuantityTimesUnitCost() {
        double expected = item.getQuantity() * item.getUnitCost();
        assertEquals(expected, item.getLineTotal(), 0.001);
    }
}
