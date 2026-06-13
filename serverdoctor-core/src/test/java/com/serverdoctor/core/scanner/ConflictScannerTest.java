package com.serverdoctor.core.scanner;

import com.serverdoctor.api.module.AnalysisResult;
import com.serverdoctor.core.conflict.ConflictDatabase;
import com.serverdoctor.core.engine.CoreServerContext;
import com.serverdoctor.testing.FakeServerPlatform;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConflictScannerTest {

    private final ConflictScanner scanner = new ConflictScanner(ConflictDatabase.withDefaults());

    @Test void reportsConflictWhenBothPluginsPresent() {
        var ctx = new CoreServerContext(FakeServerPlatform.builder()
                .plugin("LuckPerms", "5.4").plugin("PermissionsEx", "1.0").build(), null);
        AnalysisResult result = scanner.analyze(ctx);
        assertEquals(1, result.conflicts().size());
    }

    @Test void noConflictWhenOnlyOnePresent() {
        var ctx = new CoreServerContext(FakeServerPlatform.builder()
                .plugin("LuckPerms", "5.4").build(), null);
        AnalysisResult result = scanner.analyze(ctx);
        assertTrue(result.conflicts().isEmpty());
    }
}
