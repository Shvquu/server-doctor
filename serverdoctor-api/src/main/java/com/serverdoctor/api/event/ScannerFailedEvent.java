package com.serverdoctor.api.event;

import java.time.Instant;

/** Fired when a single scanner throws; the run continues with the remaining scanners. */
public record ScannerFailedEvent(String moduleId, String error, Instant timestamp)
        implements ServerDoctorEvent {
    public ScannerFailedEvent(String moduleId, String error) { this(moduleId, error, Instant.now()); }
}
