package com.serverdoctor.storage.repository;

import com.serverdoctor.common.model.ScanSummary;

import java.util.List;

public interface ScanSummaryRepository {
    void save(ScanSummary summary);
    /** Jüngste Zusammenfassungen, neueste zuerst. */
    List<ScanSummary> recent(int limit);

    /** Verwirft alles – Fallback für Provider, die keine Zusammenfassungen speichern. */
    ScanSummaryRepository NOOP = new ScanSummaryRepository() {
        @Override public void save(ScanSummary summary) { /* bewusst leer */ }
        @Override public List<ScanSummary> recent(int limit) { return List.of(); }
    };
}
