package integration;

import domain.CatalogueItem;
import org.junit.jupiter.api.*;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class InventoryServiceImplTest {

    private static InventoryServiceImpl service;

    @BeforeAll
    static void setUp() {
        new File("database").mkdirs();
        new File("sql").mkdirs();
        database.DatabaseSetup.initialise();
        service = new InventoryServiceImpl();
    }

    // ----------------------------------------------------------------
    // getCatalogue()
    // ----------------------------------------------------------------
    @Test
    @Order(1)
    @DisplayName("getCatalogue() returns non-null list")
    void testGetCatalogueNotNull() {
        List<CatalogueItem> result = service.getCatalogue();
        assertNotNull(result, "Catalogue list should not be null");
    }

    @Test
    @Order(2)
    @DisplayName("getCatalogue() items have non-null item IDs")
    void testCatalogueItemsHaveIds() {
        List<CatalogueItem> result = service.getCatalogue();
        for (CatalogueItem item : result) {
            assertNotNull(item.getItemId(), "Each item should have a non-null item ID");
        }
    }

    // ----------------------------------------------------------------
    // deductStock() — invalid parameters
    // ----------------------------------------------------------------
    @Test
    @Order(3)
    @DisplayName("deductStock() returns ERROR for unknown item ID")
    void testDeductStockUnknownItem() {
        String result = service.deductStock("UNKNOWN-ITEM-XYZ");
        assertNotNull(result);
        assertTrue(result.startsWith("ERROR"),
                "Should return ERROR for unknown item ID: " + result);
    }

    @Test
    @Order(4)
    @DisplayName("deductStock() returns ERROR for null item ID")
    void testDeductStockNull() {
        String result = service.deductStock(null);
        assertNotNull(result);
        assertTrue(result.startsWith("ERROR"),
                "Should return ERROR for null item ID");
    }

    @Test
    @Order(5)
    @DisplayName("deductStock() returns ERROR for empty item ID")
    void testDeductStockEmpty() {
        String result = service.deductStock("");
        assertNotNull(result);
        assertTrue(result.startsWith("ERROR"),
                "Should return ERROR for empty item ID");
    }
}