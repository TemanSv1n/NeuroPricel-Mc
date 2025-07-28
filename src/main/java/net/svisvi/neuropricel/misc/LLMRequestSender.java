package net.svisvi.neuropricel.misc;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.svisvi.neuropricel.init.ModServerConfigs;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class LLMRequestSender {
    private static final Gson gson = new Gson();
    public static final String TEXT_IP = ModServerConfigs.TEXT_SERVER_IP.get();

    private static String getServerUrl() {
        // Ensure the URL has http:// prefix
        String ip = TEXT_IP.trim();
        if (!ip.startsWith("http://") && !ip.startsWith("https://")) {
            return "http://" + ip;
        }
        return ip;
    }

    public static CompletableFuture<String> getAiResponseAsync(String prompt) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 1. Prepare the request payload
                JsonObject payload = new JsonObject();
                payload.addProperty("text", prompt);
                payload.addProperty("construct", "pricel");
                payload.addProperty("response_format", "short");

                // 2. Create and configure the HTTP connection
                String serverUrl = getServerUrl() + "/chat"; // Add endpoint
                HttpURLConnection conn = (HttpURLConnection) new URL(serverUrl).openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "application/json; charset=utf-8");
                conn.setConnectTimeout(60000);
                conn.setDoOutput(true);

                // 3. Send the request
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(gson.toJson(payload).getBytes(StandardCharsets.UTF_8));
                }

                // 4. Process the response
                try (InputStreamReader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
                    JsonObject response = gson.fromJson(reader, JsonObject.class);
                    return response.get("response").getAsString();
                }
            } catch (Exception e) {
                throw new RuntimeException("LLM request failed: " + e.getMessage());
            }
        });
    }
}