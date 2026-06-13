package com.serverdoctor.common.model;

/** Eine generierte Handlungsempfehlung. Reine Daten - niemals automatisch ausgeführt. */
public record Recommendation(String id, Category category, Severity severity,
                             String title, String description) {

    public enum Category { UPDATE, REPLACE, CONFIGURATION, CONFLICT, PERFORMANCE, SECURITY, MEMORY }
}
