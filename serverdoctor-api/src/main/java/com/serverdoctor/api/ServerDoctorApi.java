package com.serverdoctor.api;

import com.serverdoctor.api.event.EventBus;
import com.serverdoctor.api.module.AnalysisModule;
import com.serverdoctor.api.module.DiagnosticReport;
import com.serverdoctor.common.model.ConflictReport;
import com.serverdoctor.common.model.PerformanceSnapshot;
import com.serverdoctor.common.model.Recommendation;
import com.serverdoctor.common.model.SecurityRisk;

import java.util.List;
import java.util.Optional;

/**
 * Öffentlicher Einstiegspunkt für Fremd-Plugins.
 * Zugriff via {@link ServerDoctorProvider#get()}.
 */
public interface ServerDoctorApi {

    /** Frischer Performance-Snapshot, unabhängig vom letzten Lauf. */
    PerformanceSnapshot getPerformanceSnapshot();

    /** Der jüngste vollständige Diagnose-Report, falls bereits einer lief. */
    Optional<DiagnosticReport> getLatestReport();

    List<ConflictReport> getConflicts();

    List<SecurityRisk> getSecurityRisks();

    List<Recommendation> getRecommendations();

    /** Eigenen Scanner registrieren. */
    void registerModule(AnalysisModule module);

    void unregisterModule(String moduleId);

    EventBus events();

    /** Führt sofort eine vollständige Analyse aus und liefert den Report. */
    DiagnosticReport runDiagnostics();
}
