package com.serverdoctor.api;

import com.serverdoctor.api.event.EventBus;
import com.serverdoctor.api.module.AnalysisModule;
import com.serverdoctor.api.module.DiagnosticReport;
import com.serverdoctor.common.model.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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

    /** Aktuelle Server-Eckdaten (Plattform, Version, Java-Version). */
    ServerInfo getServerInfo();

    /** Momentaufnahme der aktuell installierten Plugins. */
    List<PluginInfo> getPlugins();

    /** Vom aktuellen Server angebotene Capabilities. */
    Set<Capability> getCapabilities();

    /** Gesamt-Schweregrad des jüngsten Reports (OK, falls noch keiner lief). */
    Severity getOverallSeverity();

    /** Zeitpunkt des letzten Analyse-Laufs, falls vorhanden. */
    Optional<Instant> getLastRunTimestamp();

    /** IDs aller aktuell registrierten Scanner. */
    List<String> getRegisteredModuleIds();

    EventBus events();

    /** Führt sofort eine vollständige Analyse aus und liefert den Report. */
    DiagnosticReport runDiagnostics();
}
