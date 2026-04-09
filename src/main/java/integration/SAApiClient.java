package integration;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * HTTP client for communicating with the IPOS-SA subsystem.
 * Base URL: http://localhost:8081/
 *
 * SA endpoints consumed:
 *   GET  /api/inventory/catalogue
 *   POST /api/inventory/deduct
 *   POST /api/orders/place
 *   GET  /api/orders/track?orderID=xxx
 *   GET  /api/orders/balance?merchantID=xxx
 *   GET  /api/orders/invoice?orderID=xxx
 *   GET  /api/orders/status?merchantID=xxx&status=xxx
 *   GET  /api/orders/discount?merchantID=xxx
 *   GET  /api/orders/credit?merchantID=xxx
 *   POST /api/membership/request
 */
public class SAApiClient {

    private static final String BASE_URL = "http://localhost:8081";
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    // ==================== INVENTORY ====================

    /**
     * Fetches the full active product catalogue from SA.
     * Returns a JSONArray of items, each with: itemId, description, packageCost, availability.
     * Returns an empty array if SA is unreachable.
     */
    public static JSONArray getCatalogue() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/inventory/catalogue"))
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return new JSONArray(response.body());
            }
        } catch (Exception e) {
            System.err.println("[SAApiClient] getCatalogue failed: " + e.getMessage());
        }
        return new JSONArray();
    }

    /**
     * Requests SA to deduct stock for a given item.
     * @param itemId   the SA item ID
     * @param quantity number of packs to deduct
     * @return true if SA confirmed the deduction
     */
    public static boolean deductStock(String itemId, int quantity) {
        try {
            JSONObject payload = new JSONObject()
                    .put("itemID", itemId)
                    .put("quantity", quantity);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/inventory/deduct"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .build();

            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            System.err.println("[SAApiClient] deductStock failed: " + e.getMessage());
            return false;
        }
    }

    // ==================== ORDERS ====================

    /**
     * Places an order via SA.
     * @param merchantId   the merchant placing the order
     * @param orderDetails order details string
     * @return the order ID assigned by SA, or null on failure
     */
    public static String placeOrder(String merchantId, String orderDetails) {
        try {
            JSONObject payload = new JSONObject()
                    .put("merchantID", merchantId)
                    .put("orderDetails", orderDetails);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/orders/place"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .build();

            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JSONObject body = new JSONObject(response.body());
                return body.optString("orderId", null);
            }
        } catch (Exception e) {
            System.err.println("[SAApiClient] placeOrder failed: " + e.getMessage());
        }
        return null;
    }

    /**
     * Tracks delivery status of an SA order.
     * @param orderId the SA order ID
     * @return tracking info string, or empty string on failure
     */
    public static String trackDelivery(String orderId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/orders/track?orderID=" + orderId))
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JSONObject body = new JSONObject(response.body());
                return body.optString("tracking", "");
            }
        } catch (Exception e) {
            System.err.println("[SAApiClient] trackDelivery failed: " + e.getMessage());
        }
        return "";
    }

    /**
     * Gets the account balance for a merchant.
     * @param merchantId the merchant ID
     * @return the balance, or -1 on failure
     */
    public static double getBalance(String merchantId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/orders/balance?merchantID=" + merchantId))
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JSONObject body = new JSONObject(response.body());
                return body.optDouble("balance", -1);
            }
        } catch (Exception e) {
            System.err.println("[SAApiClient] getBalance failed: " + e.getMessage());
        }
        return -1;
    }

    /**
     * Retrieves an invoice for an SA order.
     * @param orderId the SA order ID
     * @return invoice string, or empty string on failure
     */
    public static String getInvoice(String orderId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/orders/invoice?orderID=" + orderId))
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JSONObject body = new JSONObject(response.body());
                return body.optString("invoice", "");
            }
        } catch (Exception e) {
            System.err.println("[SAApiClient] getInvoice failed: " + e.getMessage());
        }
        return "";
    }

    /**
     * Checks whether a merchant's account status matches the expected status.
     * @param merchantId the merchant ID
     * @param status     the expected status string (e.g. "active")
     * @return true if SA confirms the status matches
     */
    public static boolean checkAccountStatus(String merchantId, String status) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/orders/status?merchantID=" + merchantId + "&status=" + status))
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JSONObject body = new JSONObject(response.body());
                return body.optBoolean("matches", false);
            }
        } catch (Exception e) {
            System.err.println("[SAApiClient] checkAccountStatus failed: " + e.getMessage());
        }
        return false;
    }

    /**
     * Gets the discount rate for a merchant.
     * @param merchantId the merchant ID
     * @return discount rate (e.g. 0.10 for 10%), or 0 on failure
     */
    public static double getDiscountRate(String merchantId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/orders/discount?merchantID=" + merchantId))
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JSONObject body = new JSONObject(response.body());
                return body.optDouble("discountRate", 0);
            }
        } catch (Exception e) {
            System.err.println("[SAApiClient] getDiscountRate failed: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Gets the credit limit for a merchant.
     * @param merchantId the merchant ID
     * @return credit limit, or 0 on failure
     */
    public static double getCreditLimit(String merchantId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/orders/credit?merchantID=" + merchantId))
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JSONObject body = new JSONObject(response.body());
                return body.optDouble("creditLimit", 0);
            }
        } catch (Exception e) {
            System.err.println("[SAApiClient] getCreditLimit failed: " + e.getMessage());
        }
        return 0;
    }

    // ==================== MEMBERSHIP ====================

    /**
     * Submits a membership application to SA.
     * @return true if SA accepted the application
     */
    public static boolean requestMembership(String companyName, String registrationNumber,
                                            String directors, String businessType,
                                            String address, String email, String fax,
                                            boolean preferPhysicalMail) {
        try {
            JSONObject payload = new JSONObject()
                    .put("companyName", companyName)
                    .put("registrationNumber", registrationNumber)
                    .put("directors", directors)
                    .put("businessType", businessType)
                    .put("address", address)
                    .put("email", email)
                    .put("fax", fax)
                    .put("preferPhysicalMail", preferPhysicalMail);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/membership/request"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .build();

            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JSONObject body = new JSONObject(response.body());
                return body.optBoolean("success", false);
            }
        } catch (Exception e) {
            System.err.println("[SAApiClient] requestMembership failed: " + e.getMessage());
        }
        return false;
    }
}