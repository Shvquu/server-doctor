package com.serverdoctor.storage;

import com.serverdoctor.api.module.DiagnosticReport;
import com.serverdoctor.storage.repository.ConflictRepository;
import com.serverdoctor.storage.repository.PerformanceRepository;
import com.serverdoctor.storage.repository.PluginRepository;
import com.serverdoctor.storage.repository.RecommendationRepository;
import com.serverdoctor.storage.repository.SecurityRepository;

import java.time.Instant;

/** Zentraler Zugriff auf alle Repositories. */
public interface StorageProvider extends AutoCloseable {

    void initialize();

    PerformanceRepository performance();
    ConflictRepository conflicts();
    SecurityRepository security();
    RecommendationRepository recommendations();
    PluginRepository plugins();

    /** Bequemer Einstieg: einen kompletten Report mit einem Aufruf persistieren. */
    default void saveReport(DiagnosticReport report) {
        Instant at = report.timestamp();
        performance().save(report.performance());
        report.conflicts().forEach(c -> conflicts().save(at, c));
        report.securityRisks().forEach(r -> security().save(at, r));
        report.recommendations().forEach(r -> recommendations().save(at, r));
    }

    @Override
    void close();
}
