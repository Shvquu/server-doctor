package com.serverdoctor.paper.platform;

import com.serverdoctor.common.model.MemoryStats;
import com.serverdoctor.platform.MetricsAdapter;
import org.bukkit.Bukkit;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

/** Sammelt TPS/MSPT (Paper/Spigot) sowie Speicher-, GC- und Thread-Metriken. */
public final class PaperMetricsAdapter implements MetricsAdapter {

    @Override
    public double[] tps() {
        try {
            return Bukkit.getServer().getTPS();
        } catch (Throwable t) {
            return new double[0];
        }
    }

    @Override
    public double mspt() {
        try {
            return Bukkit.getServer().getAverageTickTime();
        } catch (Throwable t) {
            return Double.NaN;
        }
    }

    @Override
    public MemoryStats memory() {
        Runtime rt = Runtime.getRuntime();
        long total = rt.totalMemory();
        long free = rt.freeMemory();
        long max = rt.maxMemory();
        long used = total - free;

        long gcCount = 0;
        long gcTime = 0;
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            if (gc.getCollectionCount() > 0) gcCount += gc.getCollectionCount();
            if (gc.getCollectionTime() > 0) gcTime += gc.getCollectionTime();
        }
        return new MemoryStats(used, total, max, gcCount, gcTime);
    }

    @Override
    public int threadCount() {
        ThreadMXBean threads = ManagementFactory.getThreadMXBean();
        return threads.getThreadCount();
    }
}
