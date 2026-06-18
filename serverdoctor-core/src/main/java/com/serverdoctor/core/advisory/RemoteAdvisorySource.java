package com.serverdoctor.core.advisory;

import com.serverdoctor.common.model.PluginInfo;
import com.serverdoctor.common.model.Severity;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Reads security advisories from a real, external feed at a configurable URL. JDK-only
 * (mirrors {@code UpdateChecker}). The feed is fetched and cached with a TTL; matching uses
 * plugin name (case-insensitive) plus an affected-version expression ({@link VersionSpec}).
 *
 * <p><b>Honest by design:</b> there is no canonical CVE database for Minecraft plugins, so the
 * data comes from a feed you (or the community) maintain. If the feed is unset or unreachable,
 * this source returns nothing - it never guesses and never invents advisories.
 *
 * <h3>Feed format (one advisory per line, {@code #} starts a comment)</h3>
 * <pre>
 * plugin | affected-versions | severity | id | summary | reference-url
 * </pre>
 * e.g. {@code AwesomePlugin | <1.4.0 | HIGH | GHSA-xxxx | RCE in command handler | https://...}
 * Fields after {@code severity} are optional. {@code affected-versions} examples: {@code *},
 * {@code <1.4.0}, {@code >=1.0.0,<1.2.3}, {@code =2.0.0}.
 */
public final class RemoteAdvisorySource implements AdvisorySource {

    private final String feedUrl;
    private final Duration ttl;
    private final Consumer<String> log;
    private final HttpClient http;

    private volatile List<Entry> cache;
    private volatile Instant fetchedAt = Instant.EPOCH;
    private volatile boolean warned;

    public RemoteAdvisorySource(String feedUrl, Duration refreshInterval, Consumer<String> log) {
        this.feedUrl = feedUrl;
        this.ttl = refreshInterval == null ? Duration.ofHours(6) : refreshInterval;
        this.log = log == null ? m -> {} : log;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    @Override
    public List<Advisory> findAdvisories(PluginInfo plugin) {
        List<Entry> entries = entries();
        if (entries.isEmpty() || plugin == null || plugin.name() == null) return List.of();

        String name = plugin.name().toLowerCase(Locale.ROOT);
        List<Advisory> out = new ArrayList<>();
        for (Entry e : entries) {
            if (e.plugin.equals(name) && e.spec.matches(plugin.version())) {
                out.add(new Advisory(e.id, e.severity, e.summary, e.reference));
            }
        }
        return out;
    }

    private List<Entry> entries() {
        if (feedUrl == null || feedUrl.isBlank()) return List.of();
        List<Entry> current = cache;
        if (current != null && Duration.between(fetchedAt, Instant.now()).compareTo(ttl) < 0) {
            return current;
        }
        return refresh(current);
    }

    private synchronized List<Entry> refresh(List<Entry> previous) {
        // Another thread may have refreshed while we waited on the lock.
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
                return onFailure("advisory feed returned HTTP " + resp.statusCode(), previous);
            }
            List<Entry> parsed = parse(resp.body());
            cache = parsed;
            fetchedAt = Instant.now();
            warned = false;
            return parsed;
        } catch (Exception ex) {
            return onFailure("advisory feed unreachable (" + ex.getMessage() + ")", previous);
        }
    }

    private List<Entry> onFailure(String message, List<Entry> previous) {
        if (!warned) {
            log.accept("ServerDoctor: " + message + " - skipping advisory checks for now.");
            warned = true;
        }
        // Keep stale data if we have it; otherwise nothing. Back off until the next TTL window.
        fetchedAt = Instant.now();
        if (cache == null) cache = previous == null ? List.of() : previous;
        return cache;
    }

    private static List<Entry> parse(String body) {
        List<Entry> out = new ArrayList<>();
        if (body == null) return out;
        for (String line : body.split("\\r?\\n")) {
            String s = line.trim();
            if (s.isEmpty() || s.startsWith("#")) continue;
            String[] f = s.split("\\|");
            if (f.length < 3) continue; // need at least plugin | versions | severity
            String plugin = f[0].trim().toLowerCase(Locale.ROOT);
            VersionSpec spec = VersionSpec.parse(f[1].trim());
            Severity severity = severity(f[2].trim());
            String id = f.length > 3 ? f[3].trim() : "";
            String summary = f.length > 4 ? f[4].trim() : "";
            String reference = f.length > 5 ? f[5].trim() : "";
            if (!plugin.isEmpty()) out.add(new Entry(plugin, spec, severity, id, summary, reference));
        }
        return out;
    }

    private static Severity severity(String raw) {
        try {
            return Severity.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return Severity.MEDIUM;
        }
    }

    private record Entry(String plugin, VersionSpec spec, Severity severity,
                         String id, String summary, String reference) {}
}
