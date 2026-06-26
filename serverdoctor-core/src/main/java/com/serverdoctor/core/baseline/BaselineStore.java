package com.serverdoctor.core.baseline;

import com.serverdoctor.common.model.Severity;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/** Persists a single pinned baseline as a small properties file (no database needed). */
public final class BaselineStore {

    private final Path file;

    public BaselineStore(Path file) { this.file = file; }

    public boolean exists() { return Files.isRegularFile(file); }

    public void pin(Baseline b) throws IOException {
        Properties p = new Properties();
        p.setProperty("pinnedAt", b.pinnedAt().toString());
        p.setProperty("serverVersion", b.serverVersion());
        p.setProperty("tps1m", String.valueOf(b.tps1m()));
        p.setProperty("mspt", String.valueOf(b.mspt()));
        p.setProperty("memUsedMb", String.valueOf(b.memUsedMb()));
        p.setProperty("conflicts", String.valueOf(b.conflicts()));
        p.setProperty("securityRisks", String.valueOf(b.securityRisks()));
        p.setProperty("recommendations", String.valueOf(b.recommendations()));
        for (Severity s : Severity.values())
            p.setProperty("findings." + s.name(), String.valueOf(b.findingsBySeverity().getOrDefault(s, 0)));
        StringBuilder sb = new StringBuilder();
        p.forEach((k, v) -> sb.append(k).append('=').append(v).append('\n'));
        if (file.getParent() != null) Files.createDirectories(file.getParent());
        Files.writeString(file, sb.toString(), StandardCharsets.UTF_8);
    }

    public Optional<Baseline> load() {
        if (!exists()) return Optional.empty();
        try {
            Properties p = new Properties();
            p.load(new StringReader(Files.readString(file, StandardCharsets.UTF_8)));
            Map<Severity, Integer> bySev = new EnumMap<>(Severity.class);
            for (Severity s : Severity.values())
                bySev.put(s, intOf(p.getProperty("findings." + s.name(), "0")));
            return Optional.of(new Baseline(
                    Instant.parse(p.getProperty("pinnedAt", Instant.EPOCH.toString())),
                    p.getProperty("serverVersion", ""),
                    dbl(p.getProperty("tps1m")), dbl(p.getProperty("mspt")),
                    longOf(p.getProperty("memUsedMb", "0")),
                    intOf(p.getProperty("conflicts", "0")),
                    intOf(p.getProperty("securityRisks", "0")),
                    intOf(p.getProperty("recommendations", "0")),
                    bySev));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static double dbl(String s) { try { return Double.parseDouble(s); } catch (Exception e) { return Double.NaN; } }
    private static int intOf(String s) { try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; } }
    private static long longOf(String s) { try { return Long.parseLong(s.trim()); } catch (Exception e) { return 0L; } }
}
