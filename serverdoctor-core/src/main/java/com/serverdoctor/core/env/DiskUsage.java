package com.serverdoctor.core.env;

/** Snapshot of disk usage for the server directory, plus log-file sizes. All values in bytes. */
public record DiskUsage(String path, long totalBytes, long usableBytes,
                        long logsBytes, long latestLogBytes) {

    public double usableRatio() {
        return totalBytes <= 0 ? 1.0 : (double) usableBytes / (double) totalBytes;
    }
}
