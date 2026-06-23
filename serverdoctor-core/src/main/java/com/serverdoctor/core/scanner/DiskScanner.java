package com.serverdoctor.core.scanner;

import com.serverdoctor.api.module.AnalysisModule;
import com.serverdoctor.api.module.AnalysisResult;
import com.serverdoctor.api.module.ServerContext;
import com.serverdoctor.common.model.Capability;
import com.serverdoctor.common.model.Finding;
import com.serverdoctor.common.model.Severity;
import com.serverdoctor.core.env.DiskProbe;
import com.serverdoctor.core.env.DiskUsage;
import com.serverdoctor.core.env.FilesystemDiskProbe;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Checks free disk space for the server directory and the size of the logs folder. Pure JDK and
 * read-only; runs on every platform. Self-probing by default, so no wiring is required.
 */
public final class DiskScanner implements AnalysisModule {

    private static final long GB = 1024L * 1024L * 1024L;
    private static final long MB = 1024L * 1024L;

    private final DiskProbe probe;

    public DiskScanner() { this(new FilesystemDiskProbe()); }

    public DiskScanner(DiskProbe probe) {
        this.probe = probe == null ? new FilesystemDiskProbe() : probe;
    }

    @Override public String id() { return "disk"; }

    @Override public Set<Capability> requiredCapabilities() { return Set.of(Capability.HAS_PLUGINS); }

    @Override
    public AnalysisResult analyze(ServerContext context) {
        AnalysisResult.Builder out = AnalysisResult.builder(id());
        Optional<DiskUsage> sampled = probe.sample();
        if (sampled.isEmpty()) return out.build();
        DiskUsage d = sampled.get();

        if (d.totalBytes() > 0) {
            double ratio = d.usableRatio();
            if (ratio < 0.05 || d.usableBytes() < 512 * MB) {
                out.finding(new Finding(id(), Severity.HIGH, "Low disk space: " + human(d.usableBytes())
                        + " free of " + human(d.totalBytes()) + " (" + pct(ratio) + ")"));
            } else if (ratio < 0.10 || d.usableBytes() < GB) {
                out.finding(new Finding(id(), Severity.MEDIUM, "Disk space getting low: " + human(d.usableBytes())
                        + " free of " + human(d.totalBytes()) + " (" + pct(ratio) + ")"));
            }
        }

        if (d.logsBytes() > 2 * GB) {
            out.finding(new Finding(id(), Severity.LOW, "Logs directory is large (" + human(d.logsBytes())
                    + "); consider log rotation/cleanup"));
        }
        if (d.latestLogBytes() > 500 * MB) {
            out.finding(new Finding(id(), Severity.INFO, "latest.log is very large (" + human(d.latestLogBytes())
                    + "); a plugin may be spamming the log"));
        }
        return out.build();
    }

    private static String human(long bytes) {
        if (bytes >= GB) return String.format(Locale.ROOT, "%.1f GB", bytes / (double) GB);
        if (bytes >= MB) return String.format(Locale.ROOT, "%.0f MB", bytes / (double) MB);
        return bytes + " B";
    }
    private static String pct(double ratio) { return Math.round(ratio * 100) + "% free"; }
}
