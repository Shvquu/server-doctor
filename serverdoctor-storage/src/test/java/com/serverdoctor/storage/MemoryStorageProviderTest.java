package com.serverdoctor.storage;

import com.serverdoctor.api.module.AnalysisResult;
import com.serverdoctor.api.module.DiagnosticReport;
import com.serverdoctor.common.model.ConflictReport;
import com.serverdoctor.common.model.Finding;
import com.serverdoctor.common.model.MemoryStats;
import com.serverdoctor.common.model.PerformanceSnapshot;
import com.serverdoctor.common.model.ScanSummary;
import com.serverdoctor.common.model.SecurityRisk;
import com.serverdoctor.common.model.Severity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MemoryStorageProviderTest {

    private PerformanceSnapshot snap(double tps) {
        return new PerformanceSnapshot(new double[]{tps,tps,tps}, 5.0,
                new MemoryStats(1,1,1,0,0), 10, 0, Instant.now());
    }

    @Test void returnsMostRecentFirst() {
        StorageProvider store = StorageProviders.create(StorageConfig.memory());
        store.initialize();
        store.performance().save(snap(20.0));
        store.performance().save(snap(15.0));
        assertEquals(15.0, store.performance().latest().orElseThrow().tps1m());
        assertEquals(15.0, store.performance().recent(10).get(0).tps1m());
        assertEquals(2, store.performance().recent(10).size());
    }

    @Test void limitIsRespected() {
        StorageProvider store = StorageProviders.create(StorageConfig.memory());
        store.initialize();
        for (int i = 0; i < 5; i++) store.performance().save(snap(i));
        assertEquals(3, store.performance().recent(3).size());
    }

    @Test void storesConflicts() {
        StorageProvider store = StorageProviders.create(StorageConfig.memory());
        store.initialize();
        store.conflicts().save(Instant.now(), new ConflictReport("c","A","B",Severity.HIGH,"x"));
        assertEquals(1, store.conflicts().recent(10).size());
    }

    @Test void storesScanSummariesNewestFirst() {
        StorageProvider store = StorageProviders.create(StorageConfig.memory());
        store.initialize();
        for (int i = 0; i < 5; i++) store.summaries().save(new ScanSummary(Instant.now(), i, 0, 0, 0));
        var recent = store.summaries().recent(3);
        assertEquals(3, recent.size());
        assertEquals(4, recent.get(0).findings());
    }

    @Test void saveReportWritesSummaryEvenWithoutIssues() {
        StorageProvider store = StorageProviders.create(StorageConfig.memory());
        store.initialize();
        store.saveReport(new DiagnosticReport(Instant.now(), snap(20.0), List.of(), List.of()));
        var summary = store.summaries().recent(10);
        assertEquals(1, summary.size());
        assertEquals(new ScanSummary(summary.get(0).at(), 0, 0, 0, 0), summary.get(0));
    }

    @Test void saveReportCountsIssuesButNotRegressionFindings() {
        StorageProvider store = StorageProviders.create(StorageConfig.memory());
        store.initialize();
        AnalysisResult plugins = AnalysisResult.builder("plugins")
                .finding(new Finding("plugins", Severity.LOW, "a"))
                .finding(new Finding("plugins", Severity.CRITICAL, "b"))
                .conflict(new ConflictReport("c", "A", "B", Severity.HIGH, "x"))
                .risk(new SecurityRisk("A", SecurityRisk.RiskType.OUTDATED, Severity.MEDIUM, "y"))
                .build();
        AnalysisResult regression = AnalysisResult.builder("regression")
                .finding(new Finding("regression", Severity.HIGH, "trend"))
                .build();
        Instant at = Instant.now();
        store.saveReport(new DiagnosticReport(at, snap(20.0), List.of(plugins, regression), List.of()));
        assertEquals(new ScanSummary(at, 2, 1, 1, 1), store.summaries().recent(1).get(0));
    }
}
