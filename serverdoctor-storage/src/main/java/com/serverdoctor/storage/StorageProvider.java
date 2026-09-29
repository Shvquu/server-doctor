package com.serverdoctor.storage;

import com.serverdoctor.api.module.DiagnosticReport;
import com.serverdoctor.storage.repository.*;

import java.time.Instant;

/** Zentraler Zugriff auf alle Repositories. */
public interface StorageProvider extends AutoCloseable {

    void initialize();

    PerformanceRepository performance();
    ConflictRepository conflicts();
    SecurityRepository security();
    RecommendationRepository recommendations();
    PluginRepository plugins();
    NodeRepository nodes();

    /**
     * Eine Zählerzeile pro Analyse-Lauf (Grundlage der Count-Trend-Erkennung).
     * Default verwirft alles, damit bestehende Fremd-Provider weiter kompilieren.
     */
    default ScanSummaryRepository summaries() { return ScanSummaryRepository.NOOP; }

    /** Bequemer Einstieg: einen kompletten Report mit einem Aufruf persistieren. */
    default void saveReport(DiagnosticReport report) {
        Instant at = report.timestamp();
        performance().save(report.performance());
        report.conflicts().forEach(c -> conflicts().save(at, c));
        report.securityRisks().forEach(r -> security().save(at, r));
        report.recommendations().forEach(r -> recommendations().save(at, r));
        summaries().save(ScanSummaries.of(report));
    }

    @Override
    void close();
}
