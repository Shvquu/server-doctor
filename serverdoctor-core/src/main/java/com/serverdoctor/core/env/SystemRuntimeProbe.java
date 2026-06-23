package com.serverdoctor.core.env;

import java.lang.management.ManagementFactory;
import java.util.List;

/** Reads the live JVM via the JDK management beans. */
public final class SystemRuntimeProbe implements RuntimeProbe {

    public static final SystemRuntimeProbe INSTANCE = new SystemRuntimeProbe();

    private SystemRuntimeProbe() {}

    @Override
    public RuntimeInfo sample() {
        int major = Runtime.version().feature();
        long maxHeap = Runtime.getRuntime().maxMemory();
        long totalRam = totalPhysicalMemory();
        List<String> args;
        try {
            args = ManagementFactory.getRuntimeMXBean().getInputArguments();
        } catch (Exception e) {
            args = List.of();
        }
        return new RuntimeInfo(major, maxHeap, totalRam, args);
    }

    private static long totalPhysicalMemory() {
        try {
            java.lang.management.OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
            // com.sun.management.OperatingSystemMXBean exposes getTotalMemorySize() on HotSpot
            var m = os.getClass().getMethod("getTotalMemorySize");
            m.setAccessible(true);
            Object v = m.invoke(os);
            return v instanceof Long ? (Long) v : 0L;
        } catch (Exception e) {
            try { // older JDKs name it getTotalPhysicalMemorySize()
                java.lang.management.OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
                var m = os.getClass().getMethod("getTotalPhysicalMemorySize");
                m.setAccessible(true);
                Object v = m.invoke(os);
                return v instanceof Long ? (Long) v : 0L;
            } catch (Exception ignored) {
                return 0L;
            }
        }
    }
}
