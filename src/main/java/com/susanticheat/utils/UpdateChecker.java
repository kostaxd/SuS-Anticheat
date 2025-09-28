package com.susanticheat.utils;

import com.susanticheat.core.SuSAntiCheat;
import org.bukkit.Bukkit;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

public class UpdateChecker {
    
    private final SuSAntiCheat plugin;
    private static final String UPDATE_URL = "https://api.github.com/repos/susanticheat/sus/releases/latest";
    
    public UpdateChecker(SuSAntiCheat plugin) {
        this.plugin = plugin;
    }
    
    public void checkForUpdates() {
        CompletableFuture.runAsync(() -> {
            try {
                URL url = new URL(UPDATE_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "SuS-AntiCheat-Plugin");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                
                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    // Parse JSON response (simple parsing for tag_name)
                    String jsonResponse = response.toString();
                    String latestVersion = extractVersionFromJson(jsonResponse);
                    String currentVersion = plugin.getDescription().getVersion();
                    
                    if (latestVersion != null && !latestVersion.equals(currentVersion)) {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            plugin.getLogger().info("A new version of SuS AntiCheat is available!");
                            plugin.getLogger().info("Current version: " + currentVersion);
                            plugin.getLogger().info("Latest version: " + latestVersion);
                            plugin.getLogger().info("Download: https://github.com/susanticheat/sus/releases/latest");
                        });
                    }
                }
                
                connection.disconnect();
                
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to check for updates: " + e.getMessage());
            }
        });
    }
    
    private String extractVersionFromJson(String json) {
        try {
            // Simple JSON parsing for tag_name field
            String searchFor = "\"tag_name\":\"";
            int start = json.indexOf(searchFor);
            if (start != -1) {
                start += searchFor.length();
                int end = json.indexOf("\"", start);
                if (end != -1) {
                    return json.substring(start, end);
                }
            }
        } catch (Exception e) {
            // Ignore parsing errors
        }
        return null;
    }
}