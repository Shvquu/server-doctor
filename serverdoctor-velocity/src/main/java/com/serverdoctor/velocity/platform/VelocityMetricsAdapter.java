package com.serverdoctor.velocity.platform;

import com.serverdoctor.common.model.MemoryStats;
import com.serverdoctor.platform.MetricsAdapter;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;

/** Proxies ticken keine Welt - TPS/MSPT sind daher nicht verfügbar (NaN). */
public final class VelocityMetricsAdapter implements MetricsAdapter {

    @Override public double[] tps() { return new double[0]; }
    @Override public double mspt() { return Double.NaN; }

    @Override
    public MemoryStats memory() {
        Runtime rt = Runtime.getRuntime();
        long total = rt.totalMemory();
        long free = rt.freeMemory();
        long max = rt.maxMemory();
        long gcCount = 0, gcTime = 0;
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            if (gc.getCollectionCount() > 0) gcCount += gc.getCollectionCount();
            if (gc.getCollectionTime() > 0) gcTime += gc.getCollectionTime();
        }
        return new MemoryStats(total - free, total, max, gcCount, gcTime);
    }

    @Override public int threadCount() { return ManagementFactory.getThreadMXBean().getThreadCount(); }
}
