package com.serverdoctor.core.engine;

import com.serverdoctor.api.event.AnalysisFinishedEvent;
import com.serverdoctor.api.event.EventBus;
import com.serverdoctor.api.event.PluginConflictDetectedEvent;
import com.serverdoctor.api.event.SecurityRiskDetectedEvent;
import com.serverdoctor.api.module.AnalysisModule;
import com.serverdoctor.api.module.AnalysisResult;
import com.serverdoctor.api.module.DiagnosticReport;
import com.serverdoctor.api.module.ServerContext;
import com.serverdoctor.common.model.PerformanceSnapshot;
import com.serverdoctor.common.model.MemoryStats;
import com.serverdoctor.common.model.Recommendation;
import com.serverdoctor.core.recommendation.RecommendationEngine;
import com.serverdoctor.platform.MetricsAdapter;
import com.serverdoctor.platform.ServerPlatform;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Orchestriert einen vollständigen Analyse-Lauf. */
public final class AnalysisEngine {

    private final ServerPlatform platform;
    private final ScannerRegistry registry;
    private final RecommendationEngine recommendations;
    private final EventBus eventBus;

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

    public DiagnosticReport run() {
        PerformanceSnapshot snapshot = captureSnapshot();
        ServerContext context = new CoreServerContext(platform, snapshot);

        List<AnalysisResult> results = new ArrayList<>();
        for (AnalysisModule module : registry.applicableFor(context.capabilities())) {
            try {
                AnalysisResult result = module.analyze(context);
                results.add(result);
                result.conflicts().forEach(c -> eventBus.publish(new PluginConflictDetectedEvent(c)));
                result.securityRisks().forEach(r -> eventBus.publish(new SecurityRiskDetectedEvent(r)));
            } catch (Exception ex) {
                platform.logger().error("Scanner '" + module.id() + "' ist fehlgeschlagen", ex);
            }
        }

        List<Recommendation> recs = recommendations.evaluate(results);
        DiagnosticReport report = new DiagnosticReport(Instant.now(), snapshot, results, recs);
        eventBus.publish(new AnalysisFinishedEvent(report));
        return report;
    }
}
