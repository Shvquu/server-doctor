package com.serverdoctor.common.model;

/** Immutabler Speicher-Snapshot in Bytes plus GC-Zähler. */
public record MemoryStats(long usedBytes, long committedBytes, long maxBytes,
                          long gcCount, long gcTimeMs) {

    public double usedRatio() {
        return maxBytes <= 0 ? Double.NaN : (double) usedBytes / (double) maxBytes;
    }

    public long usedMb() { return usedBytes / (1024 * 1024); }
    public long maxMb()  { return maxBytes  / (1024 * 1024); }
}
