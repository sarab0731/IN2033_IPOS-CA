package integration;

import domain.PUOrder;
import domain.PUOrder.PUOrderItem;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * HTTP client for communicating with the IPOS-PU subsystem.
 * Base URL: http://localhost:8080/
 */
public class PUApiClient {

    private static final String BASE_URL = "http://localhost:8080";
    private static final HttpClient HTTP  = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    // Orders

    /**
     * Fetches all new (orders that haven't yet been delivered) PU orders.
     * Returns an empty list if the PU system is unreachable.
     */
    public static List<PUOrder> getOnlineOrders() {
        List<PUOrder> orders = new ArrayList<>();
        try {
            System.out.println("test");
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/orders/undelivered"))
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JSONArray arr = new JSONArray(response.body());
                for (int i = 0; i < arr.length(); i++) {
                    System.out.println(arr.getJSONObject(i));
                    orders.add(parseOrder(arr.getJSONObject(i)));
                }
            }
        } catch (Exception e) {
            System.err.println("[PUApiClient] Could not reach PU system: " + e.getMessage());
        }
        return orders;
    }

    /**
     * Updates the status of a PU order.
     * @param orderId  the PU order ID
     * @param newStatus e.g. "READY_FOR_SHIPMENT", "SHIPPED", "DELIVERED"
     * @return true if the update was accepted (2xx response)
     */
    public static boolean updateOrderStatus(String orderId, String newStatus) {
        try {
            String body = new JSONObject().put("status", newStatus).toString();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/orders/" + orderId + "/status"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception e) {
            System.err.println("[PUApiClient] updateOrderStatus failed: " + e.getMessage());
            return false;
        }
    }

    // Payments

    /**
     * Processes a card payment via the PU payment gateway.
     * @return "success" or "declined", or "error" if the PU system is unreachable
     */
    public static String processCardPayment(String cardNumber, String expiry,
                                            String cvv, double amount) {
        try {
            JSONObject payload = new JSONObject()
                    .put("cardNumber", cardNumber)
                    .put("expiry", expiry)
                    .put("cvv", cvv)
                    .put("amount", amount);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/comms/payment"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .build();

            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body().trim().toLowerCase();
            if (body.contains("success")) return "success";
            if (body.contains("declined")) return "declined";
            return response.statusCode() >= 200 && response.statusCode() < 300 ? "success" : "declined";
        } catch (Exception e) {
            System.err.println("[PUApiClient] processCardPayment failed: " + e.getMessage());
            return "error";
        }
    }

    // Email

    /**
     * Sends an email via the PU email service.
     * @return true if accepted (2xx)
     */
    public static boolean sendEmail(String to, String subject, String body) {
        try {
            JSONObject payload = new JSONObject()
                    .put("to", to)
                    .put("subject", subject)
                    .put("body", body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/comms/email"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .build();

            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception e) {
            System.err.println("[PUApiClient] sendEmail failed: " + e.getMessage());
            return false;
        }
    }

    // Sync

    /**
     * Fetches pending stock changes from PU cache.
     * Returns JSON array: [{"productId":1,"pendingChange":-5}, ...]
     */
    public static JSONArray getPendingStockChanges() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/sync/pending-changes"))
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return new JSONArray(response.body());
            }
        } catch (Exception e) {
            System.err.println("[PUApiClient] getPendingStockChanges failed: " + e.getMessage());
        }
        return new JSONArray();
    }

    /**
     * Clears all pending stock changes in PU after CA has synced.
     */
    public static boolean clearAllPendingChanges() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/sync/clear-all-pending"))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception e) {
            System.err.println("[PUApiClient] clearAllPendingChanges failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Pushes CA's product catalog to PU cache.
     */
    public static boolean pushProductsToCache(String productsJson) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/sync/update-cache"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(productsJson))
                    .build();

            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("[PUApiClient] pushProductsToCache response: " + response.body());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception e) {
            System.err.println("[PUApiClient] pushProductsToCache failed: " + e.getMessage());
            return false;
        }
    }

    // JSON parsing

    private static PUOrder parseOrder(JSONObject obj) {
        String orderId         = obj.optString("orderId", obj.optString("id", ""));
        String memberName      = obj.optString("memberName", obj.optString("member_name", "Unknown"));
        String deliveryAddress = obj.optString("deliveryAddress", obj.optString("delivery_address", ""));
        String status          = obj.optString("status", "ACCEPTED");
        double totalValue      = obj.optDouble("totalValue", obj.optDouble("total_value", 0.0));

        List<PUOrderItem> items = new ArrayList<>();
        JSONArray itemArr = obj.optJSONArray("items");
        if (itemArr != null) {
            for (int i = 0; i < itemArr.length(); i++) {
                JSONObject it = itemArr.getJSONObject(i);
                items.add(new PUOrderItem(
                        it.optString("productName", it.optString("product_name", "")),
                        it.optInt("quantity", 0),
                        it.optDouble("unitPrice", it.optDouble("unit_price", 0.0))
                ));
            }
        }

        return new PUOrder(orderId, memberName, deliveryAddress, status, totalValue, items);
    }
}
