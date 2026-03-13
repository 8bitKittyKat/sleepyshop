package com.jotrorox.sleepyshop.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jotrorox.sleepyshop.listener.UpdateCheckListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import org.bukkit.plugin.Plugin;

public class UpdateChecker {

    private final Plugin plugin;
    private final String currentVersion;
    private final String hangarSlug;

    public UpdateChecker(Plugin plugin, String hangarSlug) {
        this.plugin = plugin;
        this.currentVersion = plugin.getDescription().getVersion();
        this.hangarSlug = hangarSlug;
    }

    public CompletableFuture<VersionResult> checkHangar() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String apiUrl =
                    "https://hangar.papermc.io/api/v1/projects/" +
                    hangarSlug +
                    "/versions";
                String response = makeRequest(apiUrl);

                if (response == null) {
                    return new VersionResult(
                        false,
                        null,
                        "Failed to connect to Hangar"
                    );
                }

                JsonObject json = JsonParser.parseString(
                    response
                ).getAsJsonObject();

                if (
                    json.has("result") &&
                    !json.getAsJsonArray("result").isEmpty()
                ) {
                    JsonObject latestVersionObj = json
                        .getAsJsonArray("result")
                        .get(0)
                        .getAsJsonObject();
                    String latestVersion = latestVersionObj
                        .get("name")
                        .getAsString();
                    String downloadUrl =
                        "https://hangar.papermc.io/" +
                        hangarSlug +
                        "/versions/" +
                        latestVersion;

                    boolean isOutdated =
                        compareVersions(currentVersion, latestVersion) < 0;

                    return new VersionResult(
                        isOutdated,
                        latestVersion,
                        downloadUrl
                    );
                }

                return new VersionResult(false, null, "No versions found");
            } catch (Exception e) {
                plugin
                    .getLogger()
                    .warning(
                        "Failed to check for updates on Hangar: " +
                            e.getMessage()
                    );
                return new VersionResult(
                    false,
                    null,
                    "Error: " + e.getMessage()
                );
            }
        });
    }

    public void performCheck() {
        plugin.getLogger().info("Checking for updates...");

        checkHangar().thenAccept(result -> {
            if (!plugin.isEnabled()) {
                return;
            }

            plugin
                .getServer()
                .getScheduler()
                .runTask(plugin, () -> handleCheckResult(result));
        });
    }

    private void handleCheckResult(VersionResult result) {
        if (result.outdated()) {
            plugin
                .getLogger()
                .warning("A new version is available on Hangar!");
            plugin
                .getLogger()
                .warning(
                    "Current: " +
                        currentVersion +
                        " | Latest: " +
                        result.latestVersion()
                );
            plugin.getLogger().warning("Download: " + result.downloadUrl());

            plugin
                .getServer()
                .getPluginManager()
                .registerEvents(
                    new UpdateCheckListener(plugin, result),
                    plugin
                );
        } else if (result.latestVersion() != null) {
            plugin
                .getLogger()
                .info("You are running the latest version from Hangar!");
        }
    }

    private String makeRequest(String urlString) {
        try {
            var url = new URI(urlString);
            HttpURLConnection connection = (HttpURLConnection) url
                .toURL()
                .openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                return null;
            }

            BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream())
            );
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            return response.toString();
        } catch (Exception e) {
            plugin
                .getLogger()
                .warning("HTTP request failed: " + e.getMessage());
            return null;
        }
    }

    private int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");

        int length = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < length; i++) {
            int num1 = i < parts1.length ? parseVersionNumber(parts1[i]) : 0;
            int num2 = i < parts2.length ? parseVersionNumber(parts2[i]) : 0;

            if (num1 < num2) return -1;
            if (num1 > num2) return 1;
        }
        return 0;
    }

    private int parseVersionNumber(String part) {
        try {
            return Integer.parseInt(part.replaceAll("[^0-9].*", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public record VersionResult(
        boolean outdated,
        String latestVersion,
        String downloadUrl
    ) {}
}
