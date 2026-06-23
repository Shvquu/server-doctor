package com.serverdoctor.core.scanner;

import com.serverdoctor.api.module.AnalysisModule;
import com.serverdoctor.api.module.AnalysisResult;
import com.serverdoctor.api.module.ServerContext;
import com.serverdoctor.common.model.Capability;
import com.serverdoctor.common.model.Finding;
import com.serverdoctor.common.model.Severity;
import com.serverdoctor.core.env.RuntimeInfo;
import com.serverdoctor.core.env.RuntimeProbe;
import com.serverdoctor.core.env.SystemRuntimeProbe;

import java.util.Locale;
import java.util.Set;

/**
 * Checks the Java runtime against ServerDoctor's baseline (Java 21): JVM major version, heap (Xmx)
 * vs. system RAM, and whether a modern GC is configured for large heaps. Pure JDK, read-only,
 * self-probing; runs on every platform.
 */
public final class RuntimeScanner implements AnalysisModule {

    private static final long GB = 1024L * 1024L * 1024L;
    private static final int RECOMMENDED_JAVA = 21;

    private final RuntimeProbe probe;

    public RuntimeScanner() { this(SystemRuntimeProbe.INSTANCE); }

    public RuntimeScanner(RuntimeProbe probe) {
        this.probe = probe == null ? SystemRuntimeProbe.INSTANCE : probe;
    }

    @Override public String id() { return "runtime"; }

    @Override public Set<Capability> requiredCapabilities() { return Set.of(Capability.HAS_PLUGINS); }

    @Override
    public AnalysisResult analyze(ServerContext context) {
        AnalysisResult.Builder out = AnalysisResult.builder(id());
        RuntimeInfo r = probe.sample();

        // Java version
        if (r.javaMajor() > 0 && r.javaMajor() < RECOMMENDED_JAVA) {
            Severity sev = r.javaMajor() < 17 ? Severity.HIGH : Severity.MEDIUM;
            out.finding(new Finding(id(), sev, "Java " + r.javaMajor()
                    + " detected; current Paper/Velocity (1.21.x) targets Java " + RECOMMENDED_JAVA));
        }

        // Heap vs system RAM
        if (r.totalRamBytes() > 0 && r.maxHeapBytes() > 0) {
            double ratio = (double) r.maxHeapBytes() / (double) r.totalRamBytes();
            if (ratio > 0.90) {
                out.finding(new Finding(id(), Severity.MEDIUM, "Heap (Xmx " + human(r.maxHeapBytes())
                        + ") is " + Math.round(ratio * 100) + "% of system RAM (" + human(r.totalRamBytes())
                        + "); leave headroom for the OS and off-heap memory"));
            }
        }

        // GC flags for large heaps
        if (r.maxHeapBytes() >= 4 * GB && !hasModernGc(r)) {
            out.finding(new Finding(id(), Severity.INFO,
                    "No explicit modern GC flag found for a large heap; tuned G1GC (e.g. Aikar's flags) "
                            + "or ZGC is recommended"));
        }
        return out.build();
    }

    private static boolean hasModernGc(RuntimeInfo r) {
        for (String a : r.jvmArgs()) {
            String s = a.toLowerCase(Locale.ROOT);
            if (s.contains("useg1gc") || s.contains("usezgc") || s.contains("useshenandoah")
                    || s.contains("+usez")) {
                return true;
            }
        }
        return false;
    }

    private static String human(long bytes) {
        if (bytes >= GB) return String.format(Locale.ROOT, "%.1f GB", bytes / (double) GB);
        return String.format(Locale.ROOT, "%.0f MB", bytes / (double) (1024L * 1024L));
    }
}
