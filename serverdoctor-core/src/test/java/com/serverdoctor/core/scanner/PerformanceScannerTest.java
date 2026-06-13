package com.serverdoctor.core.scanner;

import com.serverdoctor.api.module.AnalysisResult;
import com.serverdoctor.common.model.Capability;
import com.serverdoctor.common.model.PerformanceSnapshot;
import com.serverdoctor.common.model.MemoryStats;
import com.serverdoctor.common.model.Severity;
import com.serverdoctor.api.module.ServerContext;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PerformanceScannerTest {

    private final PerformanceScanner scanner = new PerformanceScanner();

    private ServerContext ctxWith(double tps, double mspt, double memRatio) {
        long max = 1000;
        MemoryStats mem = new MemoryStats((long)(max*memRatio), max, max, 0, 0);
        PerformanceSnapshot snap = new PerformanceSnapshot(new double[]{tps,tps,tps}, mspt, mem, 10, 0, Instant.now());
        return new ServerContext() {
            public Set<Capability> capabilities() { return Set.of(Capability.HAS_TICK_LOOP); }
            public boolean has(Capability c) { return capabilities().contains(c); }
            public List<com.serverdoctor.common.model.PluginInfo> plugins() { return List.of(); }
            public PerformanceSnapshot performance() { return snap; }
            public com.serverdoctor.common.model.ServerInfo serverInfo() { return null; }
        };
    }

    @Test void requiresTickLoop() {
        assertTrue(scanner.requiredCapabilities().contains(Capability.HAS_TICK_LOOP));
    }

    @Test void criticalWhenTpsVeryLow() {
        AnalysisResult r = scanner.analyze(ctxWith(8.0, 5.0, 0.2));
        assertEquals(Severity.CRITICAL, r.severity());
    }

    @Test void highWhenMemoryNearlyFull() {
        AnalysisResult r = scanner.analyze(ctxWith(20.0, 5.0, 0.97));
        assertTrue(r.severity().atLeast(Severity.HIGH));
    }

    @Test void healthyServerProducesNoFindings() {
        AnalysisResult r = scanner.analyze(ctxWith(20.0, 4.0, 0.3));
        assertEquals(Severity.OK, r.severity());
        assertTrue(r.findings().isEmpty());
    }
}
