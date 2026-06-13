package com.serverdoctor.common.model;

/** Identifiziertes Sicherheits- bzw. Wartungsrisiko. */
public record SecurityRisk(String pluginName, RiskType type, Severity severity, String description) {

    public enum RiskType { OUTDATED, UNMAINTAINED, MISSING_METADATA, SUSPICIOUS_STRUCTURE, ADVISORY }
}
