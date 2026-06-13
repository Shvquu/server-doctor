package com.serverdoctor.storage.repository;

import com.serverdoctor.common.model.PerformanceSnapshot;

import java.util.List;
import java.util.Optional;

public interface PerformanceRepository {
    void save(PerformanceSnapshot snapshot);
    /** Jüngste Snapshots, neueste zuerst. */
    List<PerformanceSnapshot> recent(int limit);
    Optional<PerformanceSnapshot> latest();
}
