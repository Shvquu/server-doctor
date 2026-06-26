package com.serverdoctor.core.report;

import com.serverdoctor.api.module.AnalysisResult;
import com.serverdoctor.api.module.DiagnosticReport;
import com.serverdoctor.common.model.ConflictReport;
import com.serverdoctor.common.model.Finding;
import com.serverdoctor.common.model.PerformanceSnapshot;
import com.serverdoctor.common.model.Recommendation;
import com.serverdoctor.common.model.SecurityRisk;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Renders a {@link DiagnosticReport} to JSON, Markdown or HTML and writes it to a file. Pure JDK,
 * read-only: produces a shareable artefact and never changes anything on the server.
 */
public final class ReportExporter {

    private static final DateTimeFormatter FILE_TS =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(java.time.ZoneId.systemDefault());

    /** Render the report to a string in the given format. */
    public String render(DiagnosticReport r, ReportFormat fmt) {
        return switch (fmt) {
            case JSON -> json(r);
            case HTML -> html(r);
            case MARKDOWN -> markdown(r);
        };
    }

    /** Render and write to {@code dir/report-<timestamp>.<ext>}; returns the file path. */
    public Path write(DiagnosticReport r, ReportFormat fmt, Path dir) throws IOException {
        Files.createDirectories(dir);
        Path file = dir.resolve("report-" + FILE_TS.format(r.timestamp()) + "." + fmt.extension());
        Files.writeString(file, render(r, fmt), StandardCharsets.UTF_8);
        return file;
    }

    // ---- Markdown -----------------------------------------------------------

    private String markdown(DiagnosticReport r) {
        StringBuilder b = new StringBuilder();
        PerformanceSnapshot p = r.performance();
        b.append("# ServerDoctor report\n\n");
        b.append("- **Generated:** ").append(r.timestamp()).append("\n");
        b.append("- **Overall severity:** ").append(r.overallSeverity().name()).append("\n");
        if (p != null) {
            b.append("- **TPS (1m):** ").append(fmt(p.tps1m()))
                    .append(" · **MSPT:** ").append(fmt(p.mspt())).append(" ms")
                    .append(" · **RAM:** ").append(p.memory().usedMb()).append("/").append(p.memory().maxMb()).append(" MB")
                    .append(" · **Players:** ").append(p.onlinePlayers()).append("\n");
        }
        b.append("- **Totals:** ").append(r.conflicts().size()).append(" conflict(s), ")
                .append(r.securityRisks().size()).append(" security risk(s), ")
                .append(r.recommendations().size()).append(" recommendation(s)\n\n");

        b.append("## Findings\n\n");
        boolean any = false;
        for (AnalysisResult res : r.results()) {
            for (Finding f : res.findings()) {
                b.append("- `").append(f.severity().name()).append("` **").append(f.scannerId()).append("** — ")
                        .append(f.message()).append("\n");
                any = true;
            }
        }
        if (!any) b.append("_No findings._\n");

        if (!r.conflicts().isEmpty()) {
            b.append("\n## Conflicts\n\n");
            for (ConflictReport c : r.conflicts())
                b.append("- `").append(c.severity().name()).append("` ").append(c.pluginA())
                        .append(" × ").append(c.pluginB()).append(" — ").append(c.description()).append("\n");
        }
        if (!r.securityRisks().isEmpty()) {
            b.append("\n## Security risks\n\n");
            for (SecurityRisk s : r.securityRisks())
                b.append("- `").append(s.severity().name()).append("` ").append(s.pluginName())
                        .append(" (").append(s.type().name()).append(") — ").append(s.description()).append("\n");
        }
        if (!r.recommendations().isEmpty()) {
            b.append("\n## Recommendations\n\n");
            for (Recommendation rec : r.recommendations())
                b.append("- `").append(rec.severity().name()).append("` **").append(rec.title()).append("** — ")
                        .append(rec.description()).append("\n");
        }
        return b.toString();
    }

    // ---- HTML ---------------------------------------------------------------

    private String html(DiagnosticReport r) {
        PerformanceSnapshot p = r.performance();
        StringBuilder b = new StringBuilder();
        b.append("<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\">")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">")
                .append("<title>ServerDoctor report</title><style>")
                .append("body{font-family:system-ui,Segoe UI,Roboto,sans-serif;margin:2rem auto;max-width:60rem;color:#1b1b1f;padding:0 1rem}")
                .append("h1{margin-bottom:.2rem}.meta{color:#555;margin-bottom:1.5rem}")
                .append("table{border-collapse:collapse;width:100%;margin:.5rem 0 1.5rem}")
                .append("th,td{text-align:left;padding:.4rem .6rem;border-bottom:1px solid #e3e3e8;vertical-align:top}")
                .append(".sev{font:600 .75rem/1 ui-monospace,monospace;padding:.15rem .4rem;border-radius:.3rem;color:#fff;white-space:nowrap}")
                .append(".OK{background:#3a3}.INFO{background:#58a}.LOW{background:#7a7}.MEDIUM{background:#d90}")
                .append(".HIGH{background:#e60}.CRITICAL{background:#c33}</style></head><body>");
        b.append("<h1>ServerDoctor report</h1><div class=\"meta\">")
                .append(esc(r.timestamp().toString())).append(" · overall ").append(sevBadge(r.overallSeverity().name()))
                .append("</div>");
        if (p != null) {
            b.append("<table><tr><th>TPS (1m)</th><th>MSPT</th><th>RAM</th><th>Players</th><th>Threads</th></tr><tr>")
                    .append("<td>").append(fmt(p.tps1m())).append("</td><td>").append(fmt(p.mspt())).append(" ms</td><td>")
                    .append(p.memory().usedMb()).append("/").append(p.memory().maxMb()).append(" MB</td><td>")
                    .append(p.onlinePlayers()).append("</td><td>").append(p.threadCount()).append("</td></tr></table>");
        }
        b.append("<h2>Findings</h2><table><tr><th>Severity</th><th>Scanner</th><th>Message</th></tr>");
        for (AnalysisResult res : r.results())
            for (Finding f : res.findings())
                b.append("<tr><td>").append(sevBadge(f.severity().name())).append("</td><td>")
                        .append(esc(f.scannerId())).append("</td><td>").append(esc(f.message())).append("</td></tr>");
        b.append("</table>");
        if (!r.conflicts().isEmpty()) {
            b.append("<h2>Conflicts</h2><table><tr><th>Severity</th><th>Plugins</th><th>Description</th></tr>");
            for (ConflictReport c : r.conflicts())
                b.append("<tr><td>").append(sevBadge(c.severity().name())).append("</td><td>")
                        .append(esc(c.pluginA())).append(" × ").append(esc(c.pluginB())).append("</td><td>")
                        .append(esc(c.description())).append("</td></tr>");
            b.append("</table>");
        }
        if (!r.recommendations().isEmpty()) {
            b.append("<h2>Recommendations</h2><table><tr><th>Severity</th><th>Title</th><th>Description</th></tr>");
            for (Recommendation rec : r.recommendations())
                b.append("<tr><td>").append(sevBadge(rec.severity().name())).append("</td><td>")
                        .append(esc(rec.title())).append("</td><td>").append(esc(rec.description())).append("</td></tr>");
            b.append("</table>");
        }
        b.append("</body></html>");
        return b.toString();
    }

    private static String sevBadge(String sev) {
        return "<span class=\"sev " + esc(sev) + "\">" + esc(sev) + "</span>";
    }

    // ---- JSON (self-contained) ----------------------------------------------

    private String json(DiagnosticReport r) {
        StringBuilder b = new StringBuilder();
        PerformanceSnapshot p = r.performance();
        b.append("{\"timestamp\":").append(js(r.timestamp().toString()))
                .append(",\"overallSeverity\":").append(js(r.overallSeverity().name()));
        if (p != null) {
            b.append(",\"performance\":{")
                    .append("\"tps1m\":").append(jn(p.tps1m()))
                    .append(",\"mspt\":").append(jn(p.mspt()))
                    .append(",\"memoryUsedMb\":").append(p.memory().usedMb())
                    .append(",\"memoryMaxMb\":").append(p.memory().maxMb())
                    .append(",\"players\":").append(p.onlinePlayers())
                    .append(",\"threads\":").append(p.threadCount()).append("}");
        }
        b.append(",\"conflicts\":[");
        join(b, r.conflicts(), c -> "{\"severity\":" + js(c.severity().name()) + ",\"pluginA\":" + js(c.pluginA())
                + ",\"pluginB\":" + js(c.pluginB()) + ",\"description\":" + js(c.description()) + "}");
        b.append("],\"securityRisks\":[");
        join(b, r.securityRisks(), s -> "{\"severity\":" + js(s.severity().name()) + ",\"plugin\":" + js(s.pluginName())
                + ",\"type\":" + js(s.type().name()) + ",\"description\":" + js(s.description()) + "}");
        b.append("],\"recommendations\":[");
        join(b, r.recommendations(), rec -> "{\"severity\":" + js(rec.severity().name()) + ",\"title\":" + js(rec.title())
                + ",\"description\":" + js(rec.description()) + "}");
        b.append("],\"findings\":[");
        java.util.List<String> fs = new java.util.ArrayList<>();
        for (AnalysisResult res : r.results())
            for (Finding f : res.findings())
                fs.add("{\"severity\":" + js(f.severity().name()) + ",\"scanner\":" + js(f.scannerId())
                        + ",\"message\":" + js(f.message()) + "}");
        b.append(String.join(",", fs));
        b.append("]}");
        return b.toString();
    }

    private static <T> void join(StringBuilder b, java.util.List<T> items, java.util.function.Function<T, String> f) {
        java.util.List<String> parts = new java.util.ArrayList<>();
        for (T t : items) parts.add(f.apply(t));
        b.append(String.join(",", parts));
    }

    private static String fmt(double v) {
        return Double.isNaN(v) ? "n/a" : String.format(Locale.ROOT, "%.2f", v);
    }
    private static String jn(double v) { return Double.isNaN(v) ? "null" : String.format(Locale.ROOT, "%.3f", v); }

    private static String js(String s) {
        if (s == null) return "null";
        StringBuilder b = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                case '\n' -> b.append("\\n");
                case '\r' -> b.append("\\r");
                case '\t' -> b.append("\\t");
                default -> { if (c < 0x20) b.append(String.format("\\u%04x", (int) c)); else b.append(c); }
            }
        }
        return b.append("\"").toString();
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
