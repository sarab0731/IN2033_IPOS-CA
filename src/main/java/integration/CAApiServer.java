package integration;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import database.DatabaseManager;
import domain.CatalogueItem;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * Embedded HTTP server exposing IPOS-CA stock and order-status endpoints
 * so that IPOS-PU can query and update CA data directly.
 *
 * Runs on port 8081.
 *
 * Endpoints:
 *   GET  /api/stock/catalogue            – full product catalogue
 *   GET  /api/stock/item/{itemId}        – single item availability
 *   GET  /api/stock/search?keyword=x     – keyword search
 *   POST /api/stock/deduct/{itemId}      – deduct stock (body: {"quantity":N}, default 1)
 *   GET  /api/orders/{orderId}/status    – restock order status
 */
public class CAApiServer {

    private static final int PORT = 8081;

    public static void start() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

            server.createContext("/api/stock/catalogue",  CAApiServer::handleCatalogue);
            server.createContext("/api/stock/item/",      CAApiServer::handleItemAvailability);
            server.createContext("/api/stock/search",     CAApiServer::handleSearch);
            server.createContext("/api/stock/deduct/",    CAApiServer::handleDeduct);
            server.createContext("/api/orders/",          CAApiServer::handleOrderStatus);

            server.setExecutor(Executors.newFixedThreadPool(4));
            server.start();

            System.out.println("[CAApiServer] Listening on port " + PORT);
        } catch (IOException e) {
            System.err.println("[CAApiServer] Failed to start: " + e.getMessage());
        }
    }

    // GET /api/stock/catalogue
    private static void handleCatalogue(HttpExchange ex) throws IOException {
        if (!ex.getRequestMethod().equalsIgnoreCase("GET")) { send(ex, 405, "Method Not Allowed"); return; }

        StockServiceImpl svc = new StockServiceImpl();
        List<CatalogueItem> items = svc.getCatalogue();

        JSONArray arr = new JSONArray();
        for (CatalogueItem item : items) arr.put(toJson(item));

        sendJson(ex, 200, arr.toString());
    }

    // GET /api/stock/item/{itemId}
    private static void handleItemAvailability(HttpExchange ex) throws IOException {
        if (!ex.getRequestMethod().equalsIgnoreCase("GET")) { send(ex, 405, "Method Not Allowed"); return; }

        String itemId = tail(ex.getRequestURI().getPath(), "/api/stock/item/");
        if (itemId.isEmpty()) { send(ex, 400, "Missing itemId"); return; }

        StockServiceImpl svc = new StockServiceImpl();
        CatalogueItem item = svc.itemAvailability(itemId);

        if (item == null) {
            send(ex, 404, new JSONObject().put("error", "Item not found: " + itemId).toString());
        } else {
            sendJson(ex, 200, toJson(item).toString());
        }
    }

    // GET /api/stock/search?keyword=x
    private static void handleSearch(HttpExchange ex) throws IOException {
        if (!ex.getRequestMethod().equalsIgnoreCase("GET")) { send(ex, 405, "Method Not Allowed"); return; }

        String keyword = queryParam(ex.getRequestURI().getRawQuery(), "keyword");
        if (keyword == null || keyword.isEmpty()) { send(ex, 400, "Missing keyword"); return; }

        StockServiceImpl svc = new StockServiceImpl();
        List<CatalogueItem> results = svc.searchStock(keyword);

        JSONArray arr = new JSONArray();
        for (CatalogueItem item : results) arr.put(toJson(item));

        sendJson(ex, 200, arr.toString());
    }

    // POST /api/stock/deduct/{itemId}
    // Optional JSON body: {"quantity": N}  (defaults to 1)
    private static void handleDeduct(HttpExchange ex) throws IOException {
        if (!ex.getRequestMethod().equalsIgnoreCase("POST")) { send(ex, 405, "Method Not Allowed"); return; }

        String itemId = tail(ex.getRequestURI().getPath(), "/api/stock/deduct/");
        if (itemId.isEmpty()) { send(ex, 400, "Missing itemId"); return; }

        int quantity = 1;
        try {
            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8).trim();
            if (!body.isEmpty()) {
                quantity = new JSONObject(body).optInt("quantity", 1);
            }
        } catch (Exception ignored) {}

        if (quantity < 1) { send(ex, 400, new JSONObject().put("error", "quantity must be >= 1").toString()); return; }

        String result = deductByQuantity(itemId, quantity);

        if (result.startsWith("ERROR")) {
            sendJson(ex, 400, new JSONObject().put("error", result).toString());
        } else {
            sendJson(ex, 200, new JSONObject().put("status", "OK").put("message", result).toString());
        }
    }

    // GET /api/orders/{orderId}/status
    private static void handleOrderStatus(HttpExchange ex) throws IOException {
        if (!ex.getRequestMethod().equalsIgnoreCase("GET")) { send(ex, 405, "Method Not Allowed"); return; }

        // Path: /api/orders/{orderId}/status
        String path = ex.getRequestURI().getPath();
        String afterPrefix = tail(path, "/api/orders/");
        // afterPrefix = "{orderId}/status"
        String orderId = afterPrefix.contains("/") ? afterPrefix.substring(0, afterPrefix.indexOf('/')) : afterPrefix;

        if (orderId.isEmpty()) { send(ex, 400, "Missing orderId"); return; }

        OrderStatusImpl svc = new OrderStatusImpl();
        String status = svc.getOrderStatus(orderId);

        if (status == null || status.startsWith("ERROR") || status.startsWith("Unknown")) {
            sendJson(ex, 404, new JSONObject().put("error", "Order not found: " + orderId).toString());
        } else {
            sendJson(ex, 200, new JSONObject().put("orderId", orderId).put("status", status).toString());
        }
    }

    // Deducts `quantity` units of `itemId` in a single UPDATE statement
    private static String deductByQuantity(String itemId, int quantity) {
        String checkSql  = "SELECT stock_quantity FROM products WHERE item_id = ?";
        String updateSql = "UPDATE products SET stock_quantity = stock_quantity - ? WHERE item_id = ? AND stock_quantity >= ?";

        try (Connection conn = DatabaseManager.getConnection()) {

            // Check existence and stock level
            int currentStock;
            try (PreparedStatement check = conn.prepareStatement(checkSql)) {
                check.setString(1, itemId);
                var rs = check.executeQuery();
                if (!rs.next()) return "ERROR: Item not found - " + itemId;
                currentStock = rs.getInt("stock_quantity");
            }

            if (currentStock < quantity) {
                return "ERROR: Insufficient stock for " + itemId + " (have " + currentStock + ", need " + quantity + ")";
            }

            try (PreparedStatement update = conn.prepareStatement(updateSql)) {
                update.setInt(1, quantity);
                update.setString(2, itemId);
                update.setInt(3, quantity);
                int rows = update.executeUpdate();
                if (rows == 0) return "ERROR: Stock update failed (race condition) for " + itemId;
            }

            return "Stock deducted: " + quantity + " unit(s) of " + itemId;

        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR: " + e.getMessage();
        }
    }

    // Helpers

    private static JSONObject toJson(CatalogueItem item) {
        return new JSONObject()
                .put("itemId",       item.getItemId())
                .put("description",  item.getDescription())
                .put("packageType",  item.getPackageType() != null ? item.getPackageType() : "")
                .put("unitsInPack",  item.getUnitsInPack())
                .put("price",        item.getPrice())
                .put("availability", item.getAvailability());
    }

    /** Returns everything after `prefix` in `path`. */
    private static String tail(String path, String prefix) {
        if (path.startsWith(prefix)) return path.substring(prefix.length());
        return "";
    }

    /** Parses a single named param from a raw query string (e.g. "keyword=paracetamol&foo=bar"). */
    private static String queryParam(String rawQuery, String name) {
        if (rawQuery == null) return null;
        for (String pair : rawQuery.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && kv[0].equals(name)) {
                return java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private static void sendJson(HttpExchange ex, int status, String json) throws IOException {
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        send(ex, status, json);
    }

    private static void send(HttpExchange ex, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }
}
