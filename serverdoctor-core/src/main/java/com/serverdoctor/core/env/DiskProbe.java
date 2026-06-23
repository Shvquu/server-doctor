package com.serverdoctor.core.env;

import java.util.Optional;

/** Supplies a {@link DiskUsage} sample. Offline-tolerant: returns empty on failure. */
public interface DiskProbe {
    Optional<DiskUsage> sample();
}
