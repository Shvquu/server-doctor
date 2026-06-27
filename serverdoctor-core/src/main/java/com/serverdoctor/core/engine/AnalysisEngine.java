package com.serverdoctor.core.engine;

import com.serverdoctor.api.event.AnalysisFinishedEvent;
import com.serverdoctor.api.event.AnalysisStartedEvent;
import com.serverdoctor.api.event.EventBus;
import com.serverdoctor.api.event.OverallSeverityChangedEvent;
import com.serverdoctor.api.event.PerformanceThresholdReachedEvent;
import com.serverdoctor.api.event.PluginConflictDetectedEvent;
import com.serverdoctor.api.event.RecommendationGeneratedEvent;
import com.serverdoctor.api.event.ScannerFailedEvent;
import com.serverdoctor.api.event.SecurityRiskDetectedEvent;
import com.serverdoctor.api.module.AnalysisModule;
import com.serverdoctor.api.module.AnalysisResult;
import com.serverdoctor.api.module.DiagnosticReport;
import com.serverdoctor.api.module.ServerContext;
import com.serverdoctor.common.model.Capability;
import com.serverdoctor.common.model.MemoryStats;
import com.serverdoctor.common.model.PerformanceSnapshot;
import com.serverdoctor.common.model.PluginInfo;
import com.serverdoctor.common.model.Recommendation;
import com.serverdoctor.common.model.ServerInfo;
import com.serverdoctor.common.model.Severity;
import com.serverdoctor.core.recommendation.RecommendationEngine;
import com.serverdoctor.platform.MetricsAdapter;
import com.serverdoctor.platform.ServerPlatform;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/** Orchestriert einen vollständigen Analyse-Lauf. */
public final class AnalysisEngine {

    private final ServerPlatform platform;
    private final ScannerRegistry registry;
    private final RecommendationEngine recommendations;
    private final EventBus eventBus;
    private final AtomicReference<Severity> lastSeverity = new AtomicReference<>(Severity.OK);

    public AnalysisEngine(ServerPlatform platform, ScannerRegistry registry,
                          RecommendationEngine recommendations, EventBus eventBus) {
        this.platform = platform;
        this.registry = registry;
        this.recommendations = recommendations;
        this.eventBus = eventBus;
    }

    public PerformanceSnapshot captureSnapshot() {
        MetricsAdapter m = platform.metrics();
        return new PerformanceSnapshot(m.tps(), m.mspt(), m.memory(), m.threadCount(),
                platform.players().onlineCount(), Instant.now());
    }

    /** Current server info (platform, version, Java). Exposed via the public API. */
    public ServerInfo serverInfo() { return platform.serverInfo(); }

    /** Snapshot of installed plugins. Exposed via the public API. */
    public List<PluginInfo> plugins() { return platform.plugins().installed(); }

    /** Capabilities the current platform offers. Exposed via the public API. */
    public Set<Capability> capabilities() { return platform.capabilities(); }

    public DiagnosticReport run() {
        eventBus.publish(new AnalysisStartedEvent());
        PerformanceSnapshot snapshot = captureSnapshot();
        ServerContext context = new CoreServerContext(platform, snapshot);

        List<AnalysisResult> results = new ArrayList<>();
        for (AnalysisModule module : registry.applicableFor(context.capabilities())) {
            try {
                AnalysisResult result = module.analyze(context);
                results.add(result);
                result.conflicts().forEach(c -> eventBus.publish(new PluginConflictDetectedEvent(c)));
                result.securityRisks().forEach(r -> eventBus.publish(new SecurityRiskDetectedEvent(r)));
                if (module.id().equals("performance") && result.severity().atLeast(Severity.MEDIUM)) {
                    String reason = result.findings().isEmpty()
                            ? "Performance threshold reached"
                            : result.findings().get(0).message();
                    eventBus.publish(new PerformanceThresholdReachedEvent(snapshot, result.severity(), reason));
                }
            } catch (Exception ex) {
                platform.logger().error("Scanner '" + module.id() + "' ist fehlgeschlagen", ex);
                eventBus.publish(new ScannerFailedEvent(module.id(), String.valueOf(ex.getMessage())));
            }
        }

        List<Recommendation> recs = recommendations.evaluate(results);
        recs.forEach(r -> eventBus.publish(new RecommendationGeneratedEvent(r)));

        DiagnosticReport report = new DiagnosticReport(Instant.now(), snapshot, results, recs);

        Severity previous = lastSeverity.getAndSet(report.overallSeverity());
        if (previous != report.overallSeverity()) {
            eventBus.publish(new OverallSeverityChangedEvent(previous, report.overallSeverity()));
        }
        eventBus.publish(new AnalysisFinishedEvent(report));
        return report;
    }
}
