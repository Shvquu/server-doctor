package com.serverdoctor.storage.repository;

import com.serverdoctor.common.model.ConflictReport;

import java.time.Instant;
import java.util.List;

public interface ConflictRepository {
    void save(Instant at, ConflictReport conflict);
    List<ConflictReport> recent(int limit);
}
