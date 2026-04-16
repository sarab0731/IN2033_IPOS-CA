package integration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CommercialMembershipServiceImplTest {

    private static CommercialMembershipServiceImpl service;

    @BeforeAll
    static void setUp() {
        service = new CommercialMembershipServiceImpl();
    }

    @Test
    void testNullCandidateReturnsFalse() {
        assertFalse(service.requestMembership(null));
    }

    @Test
    void testEmptyCandidateReturnsFalse() {
        assertFalse(service.requestMembership(new String[]{}));
    }

    @Test
    void testCandidateWithFewerThan8ElementsReturnsFalse() {
        String[] short7 = {
            "Pharma Ltd", "12345678", "J Doe", "Pharmacy",
            "1 High St", "info@pharma.com", "01234567890"
        };
        assertFalse(service.requestMembership(short7));
    }

    @Test
    void testNoExceptionForNull() {
        assertDoesNotThrow(() -> service.requestMembership(null));
    }

    @Test
    void testNoExceptionForEmpty() {
        assertDoesNotThrow(() -> service.requestMembership(new String[]{}));
    }
}
