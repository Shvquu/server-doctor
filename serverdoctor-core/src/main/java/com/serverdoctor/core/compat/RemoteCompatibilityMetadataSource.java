package com.serverdoctor.core.compat;

import com.serverdoctor.common.model.PluginInfo;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Reads plugin maintenance metadata from a real, external feed (configurable URL). JDK-only,
 * cached with a TTL, offline-tolerant - identical philosophy to the advisory source: the data
 * is maintained by you or the community, and nothing is invented.
 *
 * <h3>Feed format (one plugin per line, {@code #} starts a comment)</h3>
 * <pre>
 * plugin | last-updated(YYYY-MM-DD) | folia(yes|no|unknown) | note | reference-url
 * </pre>
 * e.g. {@code ExamplePlugin | 2024-01-01 | no | Unmaintained; breaks on 1.21 | https://...}
 * Fields after {@code plugin} are optional; leave them blank to omit.
 */
public final class RemoteCompatibilityMetadataSource implements CompatibilityMetadataSource {

    private final String feedUrl;
    private final Duration ttl;
    private final Consumer<String> log;
    private final HttpClient http;

    private volatile Map<String, CompatMetadata> cache;
    private volatile Instant fetchedAt = Instant.EPOCH;
    private volatile boolean warned;

    public RemoteCompatibilityMetadataSource(String feedUrl, Duration refreshInterval, Consumer<String> log) {
        this.feedUrl = feedUrl;
        this.ttl = refreshInterval == null ? Duration.ofHours(24) : refreshInterval;
        this.log = log == null ? m -> {} : log;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    @Override
    public Optional<CompatMetadata> lookup(PluginInfo plugin) {
        if (plugin == null || plugin.name() == null) return Optional.empty();
        Map<String, CompatMetadata> map = entries();
        return Optional.ofNullable(map.get(plugin.name().toLowerCase(Locale.ROOT)));
    }

    private Map<String, CompatMetadata> entries() {
        if (feedUrl == null || feedUrl.isBlank()) return Map.of();
        Map<String, CompatMetadata> current = cache;
        if (current != null && Duration.between(fetchedAt, Instant.now()).compareTo(ttl) < 0) {
            return current;
        }
        return refresh(current);
    }

    private synchronized Map<String, CompatMetadata> refresh(Map<String, CompatMetadata> previous) {
        if (cache != null && Duration.between(fetchedAt, Instant.now()).compareTo(ttl) < 0) {
            return cache;
        }
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(feedUrl))
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", "ServerDoctor")
                    .GET()
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                return onFailure("compatibility feed returned HTTP " + resp.statusCode(), previous);
            }
            Map<String, CompatMetadata> parsed = parse(resp.body());
            cache = parsed;
            fetchedAt = Instant.now();
            warned = false;
            return parsed;
        } catch (Exception ex) {
            return onFailure("compatibility feed unreachable (" + ex.getMessage() + ")", previous);
        }
    }

    private Map<String, CompatMetadata> onFailure(String message, Map<String, CompatMetadata> previous) {
        if (!warned) {
            log.accept("ServerDoctor: " + message + " - skipping compatibility metadata for now.");
            warned = true;
        }
        fetchedAt = Instant.now();
        if (cache == null) cache = previous == null ? Map.of() : previous;
        return cache;
    }

    private static Map<String, CompatMetadata> parse(String body) {
        Map<String, CompatMetadata> out = new HashMap<>();
        if (body == null) return out;
        for (String line : body.split("\\r?\\n")) {
            String s = line.trim();
            if (s.isEmpty() || s.startsWith("#")) continue;
            String[] f = s.split("\\|");
            String name = f[0].trim();
            if (name.isEmpty()) continue;
            LocalDate updated = parseDate(field(f, 1));
            Boolean folia = parseFolia(field(f, 2));
            String note = field(f, 3);
            String reference = field(f, 4);
            out.put(name.toLowerCase(Locale.ROOT),
                    new CompatMetadata(name, updated, folia, note, reference));
        }
        return out;
    }

    private static String field(String[] f, int i) { return f.length > i ? f[i].trim() : ""; }

    private static LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try { return LocalDate.parse(raw.trim()); } catch (Exception e) { return null; }
    }

    private static Boolean parseFolia(String raw) {
        if (raw == null) return null;
        String v = raw.trim().toLowerCase(Locale.ROOT);
        return switch (v) {
            case "yes", "true", "supported"   -> Boolean.TRUE;
            case "no", "false", "unsupported" -> Boolean.FALSE;
            default -> null; // "unknown" or blank
        };
    }
}
