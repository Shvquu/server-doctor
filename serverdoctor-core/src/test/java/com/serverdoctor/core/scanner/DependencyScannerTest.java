package com.serverdoctor.core.scanner;

import com.serverdoctor.api.module.AnalysisResult;
import com.serverdoctor.common.model.PluginInfo;
import com.serverdoctor.common.model.Severity;
import com.serverdoctor.core.engine.CoreServerContext;
import com.serverdoctor.testing.FakeServerPlatform;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DependencyScannerTest {

    private final DependencyScanner scanner = new DependencyScanner();

    @Test void flagsMissingHardDependency() {
        var withDep = new PluginInfo("MyPlugin", "1.0", List.of("A"), List.of("Vault"), List.of(), true);
        var ctx = new CoreServerContext(FakeServerPlatform.builder().plugin(withDep).build(), null);
        AnalysisResult r = scanner.analyze(ctx);
        assertTrue(r.severity().atLeast(Severity.HIGH));
        assertEquals(1, r.findings().size());
    }

    @Test void noFindingWhenDependencyPresent() {
        var withDep = new PluginInfo("MyPlugin", "1.0", List.of("A"), List.of("Vault"), List.of(), true);
        var vault = new PluginInfo("Vault", "1.7", List.of("B"), List.of(), List.of(), true);
        var ctx = new CoreServerContext(
                FakeServerPlatform.builder().plugin(withDep).plugin(vault).build(), null);
        AnalysisResult r = scanner.analyze(ctx);
        assertTrue(r.findings().isEmpty());
    }
}
