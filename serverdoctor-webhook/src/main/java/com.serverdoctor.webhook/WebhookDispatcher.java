package com.serverdoctor.webhook;

import com.serverdoctor.api.event.AnalysisFinishedEvent;
import com.serverdoctor.api.event.EventBus;
import com.serverdoctor.api.module.DiagnosticReport;
import com.serverdoctor.common.model.ConflictReport;
import com.serverdoctor.common.model.SecurityRisk;
import com.serverdoctor.common.model.Severity;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Posts diagnostic alerts to Discord/Slack/Teams webhooks.
 *
 * <p>Subscribes only to {@link AnalysisFinishedEvent} and fires on **state change**, not on
 * every scan: it alerts when the overall severity reaches the configured threshold and the
 * report's "signature" (severity + conflict/risk set) differs from the last alert, and it
 * sends a single recovery notice when the state drops back below the threshold. This avoids
 * repeating the same alert every 5 minutes.
 *
 * <p>JDK-only ({@link HttpClient}); no external dependencies.
 */
public class WebhookDispatcher {

    private final WebhookConfig config;
    private final EventBus events;
    private final String label;
    private final Consumer<String> errorLog;
    private final HttpClient client;

    private volatile String lastSignature;
    private volatile boolean alerting;

    public WebhookDispatcher(WebhookConfig config, EventBus events, String label, Consumer<String> errorLog) {
        this.config = config;
        this.events = events;
        this.label = label == null ? "" : label;
        this.errorLog = errorLog == null ? m -> {} : errorLog;
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    public void start() {
        if (!config.enabled()) return;
        List<WebhookConfig.Target> valid = config.targets().stream().filter(WebhookConfig.Target::isValid).toList();
        if (valid.isEmpty()) return;
        events.subscribe(AnalysisFinishedEvent.class, this::onFinished);
    }

    private void onFinished(AnalysisFinishedEvent event) {
        DiagnosticReport report = event.report();
        Severity severity = report.overallSeverity();

        if (severity.atLeast(config.minSeverity())) {
            String signature = signature(report);
            if (signature.equals(lastSignature)) return;
            lastSignature = signature;
            alerting = true;
            dispatch(buildAlert(report, severity));
        } else if (alerting) {
            alerting = false;
            lastSignature = null;
            dispatch(buildRecovery(report));
        }
    }

    private Notification buildAlert(DiagnosticReport r, Severity severity) {
        int conflicts = r.conflicts().size();
        int risks = r.securityRisks().size();
        int recs = r.recommendations().size();

        String header = "ServerDoctor: " + severity.name() + (label.isBlank() ? "" : " - " + label);
        String summary = conflicts + " conflict(s), " + risks + " security risk(s), " + recs + " recommendation(s)";

        List<String> details = new ArrayList<>();
        r.conflicts().stream().limit(5).forEach(c -> details.add(line(c)));
        r.securityRisks().stream().limit(5).forEach(c -> details.add(line(c)));
        if (conflicts + risks > 10) details.add("... and more - see /serverdoctor report.");

        return new Notification( severity, header, summary, details);
    }

    private Notification buildRecovery(DiagnosticReport r) {
        String header = "ServerDoctor: recovered" + (label.isBlank() ? "" : " — " + label);
        return new Notification(Severity.OK, header,
                "Overall status is back below " + config.minSeverity().name() + ".", List.of());
    }

    private void dispatch(Notification n) {
        for (WebhookConfig.Target target : config.targets()) {
            if (!target.isValid()) continue;
            String body = WebhookFormater.forType(target.type()).body(n);
            HttpRequest request = HttpRequest.newBuilder(URI.create(target.url()))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            String who = target.type() + (target.name() == null || target.name().isBlank() ? "" : "  (" + target.name() + ")");
            client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).whenComplete((resp, err) -> {
                if (err != null) {
                    errorLog.accept("Error sending webhook to " + who + ": " + err.getMessage());
                } else if (resp.statusCode() >= 300) {
                    errorLog.accept("Error sending webhook to " + who + ": " + resp.statusCode() + " " + resp.body());
                }
            });
        }
    }

    private static String line(ConflictReport c) {
        return "[" + c.severity().name() + "] " + c.pluginA() + " × " + c.pluginB() + ": " + c.description();
    }

    private static String line(SecurityRisk s) {
        return "[" + s.severity().name() + "] " + s.pluginName() + " (" + s.type().name() + "): " + s.description();
    }

    private static String signature(DiagnosticReport r) {
        String conflicts = r.conflicts().stream().map(ConflictReport::id).sorted().collect(Collectors.joining(","));
        String risks = r.securityRisks().stream().map(s -> s.pluginName() + ":" + s.type().name()).sorted().collect(Collectors.joining(","));

        return r.overallSeverity().name() + "|" + conflicts + "|" + risks + "|" + r.recommendations().size();
    }
}
