package com.serverdoctor.common.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SeverityTest {

    @Test void maxPicksHigher() {
        assertEquals(Severity.HIGH, Severity.max(Severity.LOW, Severity.HIGH));
        assertEquals(Severity.CRITICAL, Severity.max(Severity.CRITICAL, Severity.OK));
    }

    @Test void atLeastRespectsOrdering() {
        assertTrue(Severity.HIGH.atLeast(Severity.MEDIUM));
        assertTrue(Severity.MEDIUM.atLeast(Severity.MEDIUM));
        assertFalse(Severity.LOW.atLeast(Severity.HIGH));
    }
}
