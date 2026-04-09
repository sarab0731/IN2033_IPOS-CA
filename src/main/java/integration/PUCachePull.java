package integration;

import database.ProductDB;
import domain.Product;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Pulls product cache from PU on CA startup.
 * This ensures CA gets any changes made while CA was offline.
 */
public class PUCachePull {

    private static final String BASE_URL = System.getenv().getOrDefault("PU_BASE_URL", "http://localhost:8080");
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    /**
     * Pulls full product cache from PU and updates CA database.
     * Call this ONCE at startup before starting the sync scheduler.
     * @return number of products pulled and updated
     */
    public static int pullCacheFromPU() {
        System.out.println("[PUCachePull] Pulling product cache from PU...");
        
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/sync/cache"))
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                System.err.println("[PUCachePull] Failed to pull cache: HTTP " + response.statusCode());
                return 0;
            }

            JSONArray products = new JSONArray(response.body());
            int updated = 0;
            int added = 0;

            for (int i = 0; i < products.length(); i++) {
                JSONObject p = products.getJSONObject(i);
                String itemId = p.getString("itemId");
                int stockQuantity = p.getInt("stockQuantity");
                
                // Check if product exists in CA
                Product existing = ProductDB.getByItemId(itemId);
                
                if (existing != null) {
                    // Update existing product with PU data
                    existing.setStockQuantity(stockQuantity);
                    existing.setPrice(p.getDouble("price"));
                    existing.setDescription(p.getString("description"));
                    existing.setPackageType(p.optString("packageType", ""));
                    existing.setVatRate(p.optDouble("vatRate", 0.0));
                    existing.setMinStockLevel(p.optInt("minStockLevel", 0));
                    
                    if (ProductDB.updateProduct(existing)) {
                        System.out.println("[PUCachePull] Updated product " + itemId + " stock=" + stockQuantity);
                        updated++;
                    }
                } else {
                    // Add new product from PU to CA
                    Product newProduct = new Product(
                        0, // productId will be auto-generated
                        itemId,
                        p.getString("description"),
                        p.optString("packageType", ""),
                        p.optInt("unitsInPack", 1),
                        p.getDouble("price"),
                        p.optDouble("vatRate", 0.0),
                        stockQuantity,
                        p.optInt("minStockLevel", 0)
                    );
                    
                    if (ProductDB.addProduct(newProduct)) {
                        System.out.println("[PUCachePull] Added new product " + itemId + " stock=" + stockQuantity);
                        added++;
                    }
                }
            }
            
            System.out.println("[PUCachePull] Pull complete: " + updated + " updated, " + added + " added");
            return updated + added;
            
        } catch (Exception e) {
            System.err.println("[PUCachePull] Error pulling cache: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }
}
