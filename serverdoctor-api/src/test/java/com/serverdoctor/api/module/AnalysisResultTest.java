package com.serverdoctor.api.module;

import com.serverdoctor.common.model.ConflictReport;
import com.serverdoctor.common.model.Finding;
import com.serverdoctor.common.model.Severity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AnalysisResultTest {

    @Test void builderRaisesSeverityToHighestEntry() {
        AnalysisResult result = AnalysisResult.builder("test")
                .finding(new Finding("test", Severity.LOW, "a"))
                .conflict(new ConflictReport("c", "A", "B", Severity.CRITICAL, "x"))
                .finding(new Finding("test", Severity.MEDIUM, "b"))
                .build();
        assertEquals(Severity.CRITICAL, result.severity());
        assertEquals(2, result.findings().size());
        assertEquals(1, result.conflicts().size());
    }

    @Test void emptyResultIsOk() {
        AnalysisResult result = AnalysisResult.empty("test");
        assertEquals(Severity.OK, result.severity());
        assertTrue(result.findings().isEmpty());
    }

    @Test void collectionsAreImmutable() {
        AnalysisResult result = AnalysisResult.empty("test");
        assertThrows(UnsupportedOperationException.class,
                () -> result.findings().add(new Finding("x", Severity.OK, "n")));
    }
}
