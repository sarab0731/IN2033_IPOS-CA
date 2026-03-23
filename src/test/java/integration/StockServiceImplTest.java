package integration;

import domain.CatalogueItem;
import org.junit.jupiter.api.*;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class StockServiceImplTest {

    private static StockServiceImpl service;

    @BeforeAll
    static void setUp() {
        // make sure database folder exists and is initialised
        new File("database").mkdirs();
        new File("sql").mkdirs();
        database.DatabaseSetup.initialise();
        service = new StockServiceImpl();
    }

    // ----------------------------------------------------------------
    // getCatalogue()
    // ----------------------------------------------------------------
    @Test
    @Order(1)
    @DisplayName("getCatalogue() returns a non-null list")
    void testGetCatalogueNotNull() {
        List<CatalogueItem> catalogue = service.getCatalogue();
        assertNotNull(catalogue, "Catalogue should not be null");
    }

    // ----------------------------------------------------------------
    // itemAvailability() — valid item ID
    // ----------------------------------------------------------------
    @Test
    @Order(2)
    @DisplayName("itemAvailability() returns null for unknown item ID")
    void testItemAvailabilityInvalidId() {
        CatalogueItem item = service.itemAvailability("INVALID-ITEM-999");
        assertNull(item, "Should return null for an item that does not exist");
    }

    @Test
    @Order(3)
    @DisplayName("itemAvailability() returns null for empty item ID")
    void testItemAvailabilityEmptyId() {
        CatalogueItem item = service.itemAvailability("");
        assertNull(item, "Should return null for empty item ID");
    }

    // ----------------------------------------------------------------
    // searchStock() — valid keyword
    // ----------------------------------------------------------------
    @Test
    @Order(4)
    @DisplayName("searchStock() returns a list (empty or not) for any keyword")
    void testSearchStockValidKeyword() {
        List<CatalogueItem> results = service.searchStock("paracetamol");
        assertNotNull(results, "Result list should not be null");
    }

    @Test
    @Order(5)
    @DisplayName("searchStock() returns empty list for non-matching keyword")
    void testSearchStockNoMatch() {
        List<CatalogueItem> results = service.searchStock("xyznotaproduct999");
        assertNotNull(results);
        assertTrue(results.isEmpty(), "Should return empty list for unknown keyword");
    }

    @Test
    @Order(6)
    @DisplayName("searchStock() handles empty keyword without throwing")
    void testSearchStockEmptyKeyword() {
        assertDoesNotThrow(() -> service.searchStock(""),
                "searchStock() should not throw for empty keyword");
    }

    // ----------------------------------------------------------------
    // deductStock() — invalid item ID
    // ----------------------------------------------------------------
    @Test
    @Order(7)
    @DisplayName("deductStock() returns ERROR for unknown item ID")
    void testDeductStockInvalidId() {
        String result = service.deductStock("INVALID-999");
        assertNotNull(result);
        assertTrue(result.startsWith("ERROR"),
                "Should return error message for unknown item: " + result);
    }

    @Test
    @Order(8)
    @DisplayName("deductStock() returns ERROR for null item ID")
    void testDeductStockNullId() {
        String result = service.deductStock(null);
        assertNotNull(result);
        assertTrue(result.startsWith("ERROR"),
                "Should return error message for null item ID");
    }
}