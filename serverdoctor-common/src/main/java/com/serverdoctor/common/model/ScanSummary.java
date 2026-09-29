package com.serverdoctor.common.model;

import java.time.Instant;

/**
 * Zählerstände eines einzelnen Analyse-Laufs. Wird pro Lauf genau einmal gespeichert
 * (auch wenn alle Zähler 0 sind), damit sich Trends über die Zeit ablesen lassen.
 *
 * @param severeFindings Findings mit Severity HIGH oder CRITICAL
 */
public record ScanSummary(Instant at, int findings, int severeFindings,
                          int conflicts, int securityRisks) {}
