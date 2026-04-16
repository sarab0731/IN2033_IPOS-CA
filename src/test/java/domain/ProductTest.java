package domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ProductTest {

    private Product belowMin;
    private Product atMin;
    private Product aboveMin;

    @BeforeEach
    void setUp() {
        belowMin = new Product(1, "ITM-001", "Paracetamol 500mg", "Box", 24, 2.99, 0.05, 3, 10);
        atMin    = new Product(2, "ITM-002", "Ibuprofen 200mg",   "Box", 16, 3.49, 0.05, 10, 10);
        aboveMin = new Product(3, "ITM-003", "Aspirin 300mg",     "Box", 16, 1.99, 0.05, 50, 10);
    }

    @Test
    void testGetters() {
        assertEquals(1, belowMin.getProductId());
        assertEquals("ITM-001", belowMin.getItemId());
        assertEquals("Paracetamol 500mg", belowMin.getDescription());
        assertEquals("Box", belowMin.getPackageType());
        assertEquals(24, belowMin.getUnitsInPack());
        assertEquals(2.99, belowMin.getPrice(), 0.001);
        assertEquals(0.05, belowMin.getVatRate(), 0.001);
        assertEquals(3, belowMin.getStockQuantity());
        assertEquals(10, belowMin.getMinStockLevel());
    }

    @Test
    void testLowStockWhenBelowMin() {
        assertTrue(belowMin.isLowStock());
    }

    @Test
    void testLowStockWhenAtMin() {
        assertTrue(atMin.isLowStock());
    }

    @Test
    void testNotLowStockWhenAboveMin() {
        assertFalse(aboveMin.isLowStock());
    }

    @Test
    void testToString() {
        assertEquals("ITM-001 - Paracetamol 500mg", belowMin.toString());
    }

    @Test
    void testSetStockQuantity() {
        aboveMin.setStockQuantity(5);
        assertEquals(5, aboveMin.getStockQuantity());
        assertTrue(aboveMin.isLowStock());
    }

    @Test
    void testSetPrice() {
        aboveMin.setPrice(9.99);
        assertEquals(9.99, aboveMin.getPrice(), 0.001);
    }

    @Test
    void testSetDescription() {
        aboveMin.setDescription("Aspirin 75mg");
        assertEquals("Aspirin 75mg", aboveMin.getDescription());
    }

    @Test
    void testSetMinStockLevel() {
        aboveMin.setMinStockLevel(100);
        assertEquals(100, aboveMin.getMinStockLevel());
        assertTrue(aboveMin.isLowStock());
    }

    @Test
    void testSetVatRate() {
        aboveMin.setVatRate(0.20);
        assertEquals(0.20, aboveMin.getVatRate(), 0.001);
    }
}
