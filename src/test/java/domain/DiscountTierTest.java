package domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DiscountTierTest {

    private DiscountTier boundedTier;
    private DiscountTier topTier;

    @BeforeEach
    void setUp() {
        boundedTier = new DiscountTier(1, 10, 0.0,   100.0, 5.0);
        topTier     = new DiscountTier(2, 10, 200.0, null,  15.0);
    }

    @Test
    void testGetters() {
        assertEquals(1, boundedTier.getTierId());
        assertEquals(10, boundedTier.getDiscountPlanId());
        assertEquals(0.0, boundedTier.getMinSpend(), 0.001);
        assertEquals(100.0, boundedTier.getMaxSpend(), 0.001);
        assertEquals(5.0, boundedTier.getRate(), 0.001);
    }

    @Test
    void testTopTierHasNullMaxSpend() {
        assertNull(topTier.getMaxSpend());
    }

    @Test
    void testBandLabelBounded() {
        assertEquals("£0 – £100", boundedTier.bandLabel());
    }

    @Test
    void testBandLabelTopTier() {
        assertEquals("£200+", topTier.bandLabel());
    }
}
