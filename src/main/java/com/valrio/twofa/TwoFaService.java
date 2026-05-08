package com.valrio.twofa;

import org.bukkit.entity.Player;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

public class TwoFaService {
    private final ValrioTwoFAPlugin plugin;
    private final HttpClient client;

    public TwoFaService(ValrioTwoFAPlugin plugin) {
        this.plugin = plugin;
        this.client = HttpClient.newHttpClient();
    }

    public boolean verify(Player player, String code) throws IOException, InterruptedException {
        String baseUrl = plugin.getConfig().getString("api.base-url", "http://127.0.0.1:8080");
        String endpoint = plugin.getConfig().getString("api.verify-endpoint", "/api/v1/2fa/verify");
        int timeoutMs = plugin.getConfig().getInt("api.timeout-ms", 5000);

        URI uri = URI.create(baseUrl + endpoint);
        UUID uuid = player.getUniqueId();
        String payload = "uuid=" + encode(uuid.toString())
                + "&username=" + encode(player.getName())
                + "&code=" + encode(code);

        HttpRequest request = HttpRequest.newBuilder(uri)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .timeout(Duration.ofMillis(timeoutMs))
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        int status = response.statusCode();
        String body = response.body() == null ? "" : response.body().trim().toLowerCase();

        return status == 200 && (body.equals("ok") || body.equals("true") || body.contains("\"valid\":true"));
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
