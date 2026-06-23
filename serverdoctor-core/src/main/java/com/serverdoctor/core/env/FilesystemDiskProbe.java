package com.serverdoctor.core.env;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

/** Reads real disk usage for a base directory (default: working directory) and its logs folder. */
public final class FilesystemDiskProbe implements DiskProbe {

    private final File baseDir;

    public FilesystemDiskProbe() {
        this(new File(System.getProperty("user.dir", ".")));
    }

    public FilesystemDiskProbe(File baseDir) {
        this.baseDir = baseDir == null ? new File(".") : baseDir;
    }

    @Override
    public Optional<DiskUsage> sample() {
        try {
            long total = baseDir.getTotalSpace();
            long usable = baseDir.getUsableSpace();
            File logsDir = new File(baseDir, "logs");
            long logs = directorySize(logsDir.toPath());
            long latest = 0L;
            File latestLog = new File(logsDir, "latest.log");
            if (latestLog.isFile()) latest = latestLog.length();
            return Optional.of(new DiskUsage(baseDir.getAbsolutePath(), total, usable, logs, latest));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static long directorySize(Path dir) {
        if (!Files.isDirectory(dir)) return 0L;
        try (Stream<Path> s = Files.walk(dir)) {
            return s.filter(Files::isRegularFile).mapToLong(p -> {
                try { return Files.size(p); } catch (Exception e) { return 0L; }
            }).sum();
        } catch (Exception e) {
            return 0L;
        }
    }
}
