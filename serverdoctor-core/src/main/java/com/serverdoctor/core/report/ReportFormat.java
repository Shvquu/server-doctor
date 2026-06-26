package com.serverdoctor.core.report;

/** Output formats for an exported diagnostic report. */
public enum ReportFormat {
    JSON("json"), MARKDOWN("md"), HTML("html");

    private final String ext;
    ReportFormat(String ext) { this.ext = ext; }
    public String extension() { return ext; }

    public static ReportFormat fromString(String s) {
        if (s == null) return MARKDOWN;
        return switch (s.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "json" -> JSON;
            case "html", "htm" -> HTML;
            default -> MARKDOWN;
        };
    }
}
