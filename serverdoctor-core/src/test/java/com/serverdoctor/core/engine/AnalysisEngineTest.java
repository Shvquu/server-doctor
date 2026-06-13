package com.serverdoctor.core.engine;

import com.serverdoctor.api.module.DiagnosticReport;
import com.serverdoctor.common.model.Severity;
import com.serverdoctor.testing.FakeServerPlatform;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AnalysisEngineTest {

    @Test void detectsKnownConflictAndLowTps() {
        FakeServerPlatform platform = FakeServerPlatform.builder()
                .plugin("LuckPerms", "5.4")
                .plugin("PermissionsEx", "1.23")
                .tps(12.0)
                .build();

        DiagnosticReport report = ServerDoctorCore.bootstrap(platform).api().runDiagnostics();

        assertEquals(1, report.conflicts().size());
        assertEquals(Severity.CRITICAL, report.overallSeverity());
        assertFalse(report.recommendations().isEmpty());
    }

    @Test void performanceScannerSkippedWithoutTickLoop() {
        FakeServerPlatform proxy = FakeServerPlatform.builder()
                .capabilities(com.serverdoctor.common.model.Capability.HAS_PLUGINS,
                              com.serverdoctor.common.model.Capability.IS_PROXY)
                .tps(1.0) // wäre kritisch - darf aber gar nicht ausgewertet werden
                .build();

        DiagnosticReport report = ServerDoctorCore.bootstrap(proxy).api().runDiagnostics();

        boolean hasPerfFinding = report.results().stream()
                .flatMap(r -> r.findings().stream())
                .anyMatch(f -> f.scannerId().equals("performance"));
        assertFalse(hasPerfFinding, "Performance-Scanner darf ohne HAS_TICK_LOOP nicht laufen");
    }

    @Test void cleanServerIsHealthy() {
        FakeServerPlatform clean = FakeServerPlatform.builder()
                .plugin("LuckPerms", "5.4")
                .tps(20.0).mspt(3.0).memoryRatio(0.4)
                .build();
        DiagnosticReport report = ServerDoctorCore.bootstrap(clean).api().runDiagnostics();
        assertTrue(report.conflicts().isEmpty());
        assertFalse(report.overallSeverity().atLeast(Severity.HIGH));
    }
}
