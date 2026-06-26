package com.serverdoctor.webhook;

import com.serverdoctor.api.module.DiagnosticReport;
import com.serverdoctor.common.model.ConflictReport;
import com.serverdoctor.common.model.PerformanceSnapshot;
import com.serverdoctor.common.model.SecurityRisk;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Sends a periodic "health digest" — a summary of the latest report — to the configured webhook
 * targets. This complements {@link WebhookDispatcher} (which only fires on state change): a digest
 * is sent on a schedule (e.g. daily) regardless of whether anything changed. Scheduling is done by
 * the platform main; this class only formats and posts.
 *
 * <p>JDK-only ({@link HttpClient}); reuses the same per-service formatting as the alert dispatcher.
 */
public final class HealthDigest {

    private final WebhookConfig config;
    private final String label;
    private final Consumer<String> errorLog;
    private final HttpClient http;

    public HealthDigest(WebhookConfig config, String label, Consumer<String> errorLog) {
        this.config = config;
        this.label = label == null ? "" : label;
        this.errorLog = errorLog == null ? m -> {} : errorLog;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    /** Build and post a digest for the given report. No-op if webhooks are disabled or no targets. */
    public void send(DiagnosticReport report) {
        if (!config.enabled() || report == null) return;
        List<WebhookConfig.Target> valid = config.targets().stream().filter(WebhookConfig.Target::isValid).toList();
        if (valid.isEmpty()) return;
        Notification n = buildDigest(report);
        for (WebhookConfig.Target target : valid) {
            String body = WebhookFormater.forType(target.type()).body(n);
            HttpRequest req = HttpRequest.newBuilder(URI.create(target.url()))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            String who = target.type() + (target.name() == null || target.name().isBlank() ? "" : " (" + target.name() + ")");
            http.sendAsync(req, HttpResponse.BodyHandlers.ofString()).whenComplete((resp, err) -> {
                if (err != null) errorLog.accept("Digest " + who + " failed: " + err.getMessage());
                else if (resp.statusCode() >= 300) errorLog.accept("Digest " + who + " returned HTTP " + resp.statusCode());
            });
        }
    }

    Notification buildDigest(DiagnosticReport r) {
        PerformanceSnapshot p = r.performance();
        String header = "ServerDoctor digest" + (label.isBlank() ? "" : " — " + label);

        StringBuilder summary = new StringBuilder("Status ").append(r.overallSeverity().name());
        if (p != null) {
            summary.append(" · TPS ").append(fmt(p.tps1m()))
                    .append(" · MSPT ").append(fmt(p.mspt())).append("ms")
                    .append(" · RAM ").append(p.memory().usedMb()).append("/").append(p.memory().maxMb()).append("MB")
                    .append(" · ").append(p.onlinePlayers()).append(" players");
        }
        summary.append(" · ").append(r.conflicts().size()).append(" conflict(s), ")
                .append(r.securityRisks().size()).append(" risk(s), ")
                .append(r.recommendations().size()).append(" recommendation(s).");

        List<String> details = new ArrayList<>();
        r.conflicts().stream().limit(3).forEach(c -> details.add(line(c)));
        r.securityRisks().stream().limit(3).forEach(s -> details.add(line(s)));
        return new Notification(r.overallSeverity(), header, summary.toString(), details);
    }

    private static String line(ConflictReport c) {
        return "[" + c.severity().name() + "] " + c.pluginA() + " × " + c.pluginB() + ": " + c.description();
    }
    private static String line(SecurityRisk s) {
        return "[" + s.severity().name() + "] " + s.pluginName() + " (" + s.type().name() + "): " + s.description();
    }
    private static String fmt(double v) {
        return Double.isNaN(v) ? "n/a" : String.format(Locale.ROOT, "%.2f", v);
    }
}
