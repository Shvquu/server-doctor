package com.serverdoctor.common.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VersionsTest {

    @Test void detectsOlderVersion() {
        assertTrue(Versions.isOlder("1.2.0", "1.2.1"));
        assertTrue(Versions.isOlder("1.0", "2.0"));
    }

    @Test void equalVersionsAreNotOlder() {
        assertEquals(0, Versions.compare("1.2.3", "1.2.3"));
        assertFalse(Versions.isOlder("1.2.3", "1.2.3"));
    }

    @Test void handlesDifferentSegmentCounts() {
        assertEquals(0, Versions.compare("1.2", "1.2.0"));
        assertTrue(Versions.compare("1.2.1", "1.2") > 0);
    }

    @Test void toleratesSuffixesAndNulls() {
        assertEquals(0, Versions.compare("1.0.0-SNAPSHOT", "1.0.0"));
        assertEquals(0, Versions.compare(null, "0"));
    }
}
