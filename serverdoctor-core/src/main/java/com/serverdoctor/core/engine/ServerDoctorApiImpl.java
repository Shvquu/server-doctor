package com.serverdoctor.core.engine;

import com.serverdoctor.api.ServerDoctorApi;
import com.serverdoctor.api.event.EventBus;
import com.serverdoctor.api.module.AnalysisModule;
import com.serverdoctor.api.module.DiagnosticReport;
import com.serverdoctor.common.model.ConflictReport;
import com.serverdoctor.common.model.PerformanceSnapshot;
import com.serverdoctor.common.model.Recommendation;
import com.serverdoctor.common.model.SecurityRisk;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/** Konkrete Public-API-Implementierung über Engine, Registry und EventBus. */
public final class ServerDoctorApiImpl implements ServerDoctorApi {

    private final AnalysisEngine engine;
    private final ScannerRegistry registry;
    private final EventBus eventBus;
    private final AtomicReference<DiagnosticReport> latest = new AtomicReference<>();

    public ServerDoctorApiImpl(AnalysisEngine engine, ScannerRegistry registry, EventBus eventBus) {
        this.engine = engine;
        this.registry = registry;
        this.eventBus = eventBus;
    }

    @Override
    public PerformanceSnapshot getPerformanceSnapshot() { return engine.captureSnapshot(); }

    @Override
    public Optional<DiagnosticReport> getLatestReport() { return Optional.ofNullable(latest.get()); }

    @Override
    public List<ConflictReport> getConflicts() {
        return getLatestReport().map(DiagnosticReport::conflicts).orElseGet(List::of);
    }

    @Override
    public List<SecurityRisk> getSecurityRisks() {
        return getLatestReport().map(DiagnosticReport::securityRisks).orElseGet(List::of);
    }

    @Override
    public List<Recommendation> getRecommendations() {
        return getLatestReport().map(DiagnosticReport::recommendations).orElseGet(List::of);
    }

    @Override public void registerModule(AnalysisModule module) { registry.register(module); }

    @Override public void unregisterModule(String moduleId) { registry.unregister(moduleId); }

    @Override public EventBus events() { return eventBus; }

    @Override
    public DiagnosticReport runDiagnostics() {
        DiagnosticReport report = engine.run();
        latest.set(report);
        return report;
    }
}
