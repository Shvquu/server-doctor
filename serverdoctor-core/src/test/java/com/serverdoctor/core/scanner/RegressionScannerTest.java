package com.serverdoctor.core.scanner;

import com.serverdoctor.api.module.AnalysisResult;
import com.serverdoctor.common.model.Finding;
import com.serverdoctor.common.model.MemoryStats;
import com.serverdoctor.common.model.PerformanceSnapshot;
import com.serverdoctor.common.model.ScanSummary;
import com.serverdoctor.common.model.Severity;
import com.serverdoctor.core.engine.CoreServerContext;
import com.serverdoctor.core.regression.NoopPerformanceHistory;
import com.serverdoctor.core.regression.PerformanceHistory;
import com.serverdoctor.core.regression.ScanHistory;
import com.serverdoctor.testing.FakeServerPlatform;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RegressionScannerTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

    private final CoreServerContext ctx = new CoreServerContext(FakeServerPlatform.builder().build(), null);

    /** First half old values, second half new values, oldest first (scanner re-sorts by time). */
    private static List<ScanSummary> scans(int[] oldRow, int[] newRow, int perHalf) {
        List<ScanSummary> out = new ArrayList<>();
        for (int i = 0; i < perHalf * 2; i++) {
            int[] r = i < perHalf ? oldRow : newRow;
            out.add(new ScanSummary(T0.plusSeconds(i * 120L), r[0], r[1], r[2], r[3]));
        }
        return out;
    }

    private AnalysisResult analyze(List<ScanSummary> scans) {
        ScanHistory history = limit -> scans;
        return new RegressionScanner(NoopPerformanceHistory.INSTANCE, history).analyze(ctx);
    }

    @Test void staysQuietWithTooFewScans() {
        var result = analyze(scans(new int[]{0, 0, 0, 0}, new int[]{9, 9, 9, 9}, 3));
        assertTrue(result.findings().isEmpty());
    }

    @Test void staysQuietWhenCountsAreStable() {
        var result = analyze(scans(new int[]{4, 1, 2, 0}, new int[]{4, 1, 2, 0}, 10));
        assertTrue(result.findings().isEmpty());
    }

    @Test void reportsRisingConflictsAsMedium() {
        var result = analyze(scans(new int[]{3, 0, 0, 0}, new int[]{3, 0, 2, 0}, 10));
        assertEquals(1, result.findings().size());
        Finding f = result.findings().get(0);
        assertEquals(Severity.MEDIUM, f.severity());
        assertTrue(f.message().startsWith("Issue count regression"), f.message());
        assertTrue(f.message().contains("Conflicts 0.0 -> 2.0 (new)"), f.message());
    }

    @Test void risingSevereFindingsIsHigh() {
        var result = analyze(scans(new int[]{2, 0, 0, 0}, new int[]{2, 1, 0, 0}, 10));
        assertEquals(Severity.HIGH, result.findings().get(0).severity());
        assertTrue(result.findings().get(0).message().contains("HIGH+ findings"));
    }

    @Test void risingSecurityRisksIsHigh() {
        var result = analyze(scans(new int[]{0, 0, 0, 1}, new int[]{0, 0, 0, 2}, 10));
        assertEquals(Severity.HIGH, result.findings().get(0).severity());
    }

    @Test void smallAbsoluteRiseIsIgnored() {
        // 0.2 -> 0.6 conflicts on average: +200 %, but below the +1.0 absolute minimum
        List<ScanSummary> list = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            int conflicts = i < 10 ? (i % 5 == 0 ? 1 : 0) : (i % 5 < 3 ? 1 : 0);
            list.add(new ScanSummary(T0.plusSeconds(i * 120L), 0, 0, conflicts, 0));
        }
        assertTrue(analyze(list).findings().isEmpty());
    }

    @Test void smallRelativeRiseIsIgnored() {
        // 10 -> 12 findings: +2 absolute, but only +20 %
        var result = analyze(scans(new int[]{10, 0, 0, 0}, new int[]{12, 0, 0, 0}, 10));
        assertTrue(result.findings().isEmpty());
    }

    @Test void performanceAndCountRegressionAreSeparateFindings() {
        List<PerformanceSnapshot> perf = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            double tps = i < 10 ? 20.0 : 14.0;
            perf.add(new PerformanceSnapshot(new double[]{tps, tps, tps}, 10.0,
                    new MemoryStats(1, 1, 1, 0, 0), 10, 0, T0.plusSeconds(i * 120L)));
        }
        PerformanceHistory perfHistory = limit -> perf;
        List<ScanSummary> counts = scans(new int[]{0, 0, 0, 0}, new int[]{0, 0, 3, 0}, 10);

        var result = new RegressionScanner(perfHistory, limit -> counts).analyze(ctx);

        assertEquals(2, result.findings().size());
        assertTrue(result.findings().get(0).message().startsWith("Performance regression"));
        assertTrue(result.findings().get(1).message().startsWith("Issue count regression"));
    }
}
