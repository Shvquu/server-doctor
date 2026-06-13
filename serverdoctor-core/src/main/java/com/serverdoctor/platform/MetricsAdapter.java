package com.serverdoctor.platform;

import com.serverdoctor.common.model.MemoryStats;

/** Liefert rohe Laufzeit-Metriken. Auf Proxies sind tps/mspt typischerweise NaN. */
public interface MetricsAdapter {
    double[] tps();
    double mspt();
    MemoryStats memory();
    int threadCount();
}
