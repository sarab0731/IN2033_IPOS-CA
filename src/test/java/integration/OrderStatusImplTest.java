package integration;

import org.junit.jupiter.api.*;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OrderStatusImplTest {

    private static OrderStatusImpl service;

    @BeforeAll
    static void setUp() {
        new File("database").mkdirs();
        new File("sql").mkdirs();
        database.DatabaseSetup.initialise();
        service = new OrderStatusImpl();
    }

    // ----------------------------------------------------------------
    // getOrderStatus() — valid order number
    // ----------------------------------------------------------------
    @Test
    @Order(1)
    @DisplayName("getOrderStatus() returns ERROR for unknown order number")
    void testGetOrderStatusUnknown() {
        String result = service.getOrderStatus("RST-UNKNOWN-999");
        assertNotNull(result);
        assertTrue(result.startsWith("ERROR"),
                "Should return ERROR for unknown order: " + result);
    }

    @Test
    @Order(2)
    @DisplayName("getOrderStatus() returns ERROR for null order ID")
    void testGetOrderStatusNull() {
        String result = service.getOrderStatus(null);
        assertNotNull(result);
        assertTrue(result.startsWith("ERROR"),
                "Should return ERROR for null order ID");
    }

    @Test
    @Order(3)
    @DisplayName("getOrderStatus() returns ERROR for empty order ID")
    void testGetOrderStatusEmpty() {
        String result = service.getOrderStatus("");
        assertNotNull(result);
        assertTrue(result.startsWith("ERROR"),
                "Should return ERROR for empty order ID");
    }

    @Test
    @Order(4)
    @DisplayName("getOrderStatus() does not throw for any input")
    void testGetOrderStatusNoException() {
        assertDoesNotThrow(() -> service.getOrderStatus("ANY-INPUT"),
                "getOrderStatus() should never throw an exception");
    }
}