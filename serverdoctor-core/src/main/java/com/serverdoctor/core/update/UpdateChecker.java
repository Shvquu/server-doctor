package com.serverdoctor.core.update;

import com.serverdoctor.common.util.Versions;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdateChecker {

    private static final Pattern TAG = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"");

    private final String repository;
    private final String currentVersion;
    private final Duration timeout;

    public UpdateChecker(String repository, String currentVersion) {
        this(repository, currentVersion, Duration.ofSeconds(8));
    }

    public UpdateChecker(String repository, String currentVersion, Duration timeout) {
        this.repository = repository;
        this.currentVersion = currentVersion;
        this.timeout = timeout;
    }

    public UpdateResult check() {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(timeout)
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.github.com/repos/" + repository + "/releases/latest"))
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "ServerDoctor-UpdateChecker")
                    .timeout(timeout)
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            int code = response.statusCode();
            if (code == 404) {
                return new UpdateResult(UpdateResult.Status.NO_RELEASES, currentVersion, null, null,
                        "Keine Releases gefunden");
            }
            if (code != 200) {
                return UpdateResult.error(currentVersion, "HTTP " + code);
            }

            String latestTag = parseLatestTag(response.body());
            if (latestTag == null) {
                return UpdateResult.error(currentVersion, "Feld 'tag_name' nicht gefunden");
            }
            String releaseUrl = "https://github.com/" + repository + "/releases/tag/" + latestTag;

            if (isNewer(latestTag, currentVersion)) {
                return new UpdateResult(UpdateResult.Status.UPDATE_AVAILABLE,
                        currentVersion, latestTag, releaseUrl, null);
            }
            return new UpdateResult(UpdateResult.Status.UP_TO_DATE,
                    currentVersion, latestTag, releaseUrl, null);
        } catch (Exception exception) {
            return UpdateResult.error(currentVersion, exception.getClass().getSimpleName() + ": " + exception.getMessage());
        }
    }

    private boolean isNewer(String latestTag, String currentVersion) {
        return Versions.isNewer(latestTag, currentVersion);
    }

    private String parseLatestTag(String json) {
        if (json == null) return null;
        Matcher matcher = TAG.matcher(json);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String strip(String version) {
        if (version == null) return null;
        String v = version.trim();
        if (!v.isEmpty() && (v.charAt(0) == 'v' || v.charAt(0) == 'V')) v = v.substring(1);
        return v;
    }

}
