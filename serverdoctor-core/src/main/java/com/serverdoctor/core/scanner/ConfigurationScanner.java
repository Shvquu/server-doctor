package com.serverdoctor.core.scanner;

import com.serverdoctor.api.module.AnalysisModule;
import com.serverdoctor.api.module.AnalysisResult;
import com.serverdoctor.api.module.ServerContext;
import com.serverdoctor.common.model.Capability;
import com.serverdoctor.common.model.Finding;
import com.serverdoctor.common.model.Severity;
import com.serverdoctor.core.config.ConfigSnapshot;
import com.serverdoctor.core.config.ConfigSource;
import com.serverdoctor.core.config.NoopConfigSource;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Reviews well-known server config files for settings that commonly cause performance or safety
 * problems, and emits recommendations. Conservative by design: every check only fires when the
 * key is actually present, and the messages are recommendations - servers differ.
 *
 * <p>Runs on all platforms via an injected {@link ConfigSource} (file reader). On a given server
 * only the files that exist are inspected (e.g. velocity.toml on Velocity, paper-*.yml on Paper).
 */
public final class ConfigurationScanner implements AnalysisModule {

    private final ConfigSource source;

    public ConfigurationScanner() {
        this(NoopConfigSource.INSTANCE);
    }

    public ConfigurationScanner(ConfigSource source) {
        this.source = source == null ? NoopConfigSource.INSTANCE : source;
    }

    @Override public String id() { return "configuration"; }

    @Override public Set<Capability> requiredCapabilities() { return Set.of(Capability.HAS_PLUGINS); }

    @Override
    public AnalysisResult analyze(ServerContext context) {
        AnalysisResult.Builder out = AnalysisResult.builder(id());
        ConfigSnapshot c = source.read();
        if (c.isEmpty()) return out.build();

        // --- server.properties ---
        intMax(out, c, "server.properties", "view-distance", 12, Severity.MEDIUM,
                "high view-distance raises CPU & bandwidth; consider 8-12 (no-tick-view-distance can extend the visible range cheaply)");
        intMax(out, c, "server.properties", "simulation-distance", 10, Severity.LOW,
                "high simulation-distance is heavy; 6-10 is usually enough");

        // --- spigot.yml ---
        intMax(out, c, "spigot.yml", "world-settings.default.mob-spawn-range", 8, Severity.LOW,
                "large mob-spawn-range increases spawn load; 6-8 is typical");
        intMax(out, c, "spigot.yml", "world-settings.default.view-distance", 12, Severity.MEDIUM,
                "per-world view-distance override is high; consider 8-12");

        // --- bukkit.yml ---
        intMax(out, c, "bukkit.yml", "spawn-limits.monsters", 70, Severity.LOW,
                "high monster spawn-limit increases entity/AI load");

        // --- paper-world(-defaults).yml ---
        intMax(out, c, "paper-world.yml", "entities.spawning.spawn-limits.monsters", 70, Severity.LOW,
                "high monster spawn-limit increases entity/AI load");

        // --- paper-global.yml (known exploits) ---
        bool(out, c, "paper-global.yml", "unsupported-settings.allow-piston-duplication", true, Severity.HIGH,
                "piston duplication is enabled (item dupes); disable unless you truly need it");

        // --- velocity.toml ---
        match(out, c, "velocity.toml", "player-info-forwarding-mode",
                v -> v.equalsIgnoreCase("none") || v.equalsIgnoreCase("legacy"), Severity.MEDIUM,
                "use 'modern' forwarding (with a secret) so backends get the real player IP/UUID and to prevent spoofing");
        match(out, c, "velocity.toml", "online-mode",
                v -> v.equalsIgnoreCase("false"), Severity.INFO,
                "online-mode=false disables Mojang authentication; make sure this is intentional");

        return out.build();
    }

    // ---- rule helpers -------------------------------------------------------

    private void intMax(AnalysisResult.Builder out, ConfigSnapshot c, String file, String key,
                        int max, Severity sev, String advice) {
        Optional<String> v = c.get(file, key);
        if (v.isEmpty()) return;
        Integer n = asInt(v.get());
        if (n != null && n > max) {
            out.finding(new Finding(id(), sev, file + ": " + key + "=" + n + " -> " + advice));
        }
    }

    private void bool(AnalysisResult.Builder out, ConfigSnapshot c, String file, String key,
                      boolean bad, Severity sev, String advice) {
        Optional<String> v = c.get(file, key);
        if (v.isPresent() && Boolean.parseBoolean(v.get().trim()) == bad) {
            out.finding(new Finding(id(), sev, file + ": " + key + "=" + v.get().trim() + " -> " + advice));
        }
    }

    private void match(AnalysisResult.Builder out, ConfigSnapshot c, String file, String key,
                       Predicate<String> bad, Severity sev, String advice) {
        Optional<String> v = c.get(file, key);
        if (v.isPresent() && bad.test(v.get().trim())) {
            out.finding(new Finding(id(), sev, file + ": " + key + "=" + v.get().trim() + " -> " + advice));
        }
    }

    private static Integer asInt(String raw) {
        if (raw == null) return null;
        try { return (int) Double.parseDouble(raw.trim()); }
        catch (NumberFormatException e) { return null; }
    }
}
