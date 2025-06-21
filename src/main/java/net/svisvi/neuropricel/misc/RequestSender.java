package net.svisvi.neuropricel.misc;

import net.svisvi.neuropricel.init.ModServerConfigs;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

public class RequestSender {
    // Config values
    public static final String VOICE_IP = ModServerConfigs.VOICE_SERVER_IP.get();
    public static final String PRICEL_SPEAKER = ModServerConfigs.PRICEL_SPEAKER.get();
    private static final Gson gson = new Gson();

    // Async TTS generation (returns Future)
    public static CompletableFuture<String> generateTTSAsync(String text) {
        System.out.println("GOOOOOON gen");
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 1. Prepare request
                String serverUrl = "http://" + VOICE_IP;
                JsonObject payload = new JsonObject();
                payload.addProperty("input_text", text);
                payload.addProperty("speaker", PRICEL_SPEAKER);
                payload.addProperty("speed", 0.3f);
                payload.addProperty("emotion", "Angry");
                payload.addProperty("gain", 13.0f);
                payload.addProperty("request_id", Instant.now().getEpochSecond());

                // 2. Send request
                HttpURLConnection conn = (HttpURLConnection) new URL(serverUrl).openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setConnectTimeout(60000);
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(gson.toJson(payload).getBytes(StandardCharsets.UTF_8));
                }

                // 3. Process response
                try (InputStreamReader reader = new InputStreamReader(conn.getInputStream())) {
                    JsonObject response = gson.fromJson(reader, JsonObject.class);
                    if (!"success".equals(response.get("status").getAsString())) {
                        throw new IOException("Server error: " + response.get("error"));
                    }
                    String output = response.get("output").getAsString();
                    System.out.println("GOON EDGE   " + serverUrl + "/output/" + output.substring(output.lastIndexOf('/') + 1));
                    return serverUrl + "/output/" + output.substring(output.lastIndexOf('/') + 1);
                }
            } catch (Exception e) {
                throw new RuntimeException("TTS failed: " + e.getMessage());
            }
        });
    }
}