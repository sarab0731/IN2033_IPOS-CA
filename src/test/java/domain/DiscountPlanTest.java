package domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DiscountPlanTest {

    private DiscountPlan fixedPlan;
    private DiscountPlan flexiblePlan;

    @BeforeEach
    void setUp() {
        fixedPlan    = new DiscountPlan(1, "Standard Fixed",  "FIXED",    10.0, "10% off everything");
        flexiblePlan = new DiscountPlan(2, "Tiered Flexible", "FLEXIBLE",  0.0, "Spend-based tiers");
    }

    @Test
    void testGetters() {
        assertEquals(1, fixedPlan.getDiscountPlanId());
        assertEquals("Standard Fixed", fixedPlan.getPlanName());
        assertEquals("FIXED", fixedPlan.getPlanType());
        assertEquals(10.0, fixedPlan.getDiscountPercent(), 0.001);
        assertEquals("10% off everything", fixedPlan.getNotes());
    }

    @Test
    void testIsFixed() {
        assertTrue(fixedPlan.isFixed());
        assertFalse(flexiblePlan.isFixed());
    }

    @Test
    void testIsFlexible() {
        assertFalse(fixedPlan.isFlexible());
        assertTrue(flexiblePlan.isFlexible());
    }

    @Test
    void testNoIdConstructor() {
        DiscountPlan plan = new DiscountPlan("New Plan", "FIXED", 5.0, "Note");
        assertEquals(0, plan.getDiscountPlanId());
        assertEquals("New Plan", plan.getPlanName());
        assertEquals("FIXED", plan.getPlanType());
        assertEquals(5.0, plan.getDiscountPercent(), 0.001);
    }

    @Test
    void testToString() {
        String result = fixedPlan.toString();
        assertTrue(result.contains("Standard Fixed"));
        assertTrue(result.contains("FIXED"));
        assertTrue(result.contains("10.0"));
    }
}
