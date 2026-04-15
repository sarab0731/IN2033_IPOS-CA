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


public class PUCachePull {

    public static int pullCacheFromPU() {
        System.out.println("[PUCachePull] Skipped.");
        return 0;
    }

}