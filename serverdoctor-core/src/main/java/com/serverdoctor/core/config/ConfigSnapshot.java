package com.serverdoctor.core.config;

import java.util.Map;
import java.util.Optional;

/** Neutral, read-only view of parsed server config files: logical file name -> (dotted key -> value). */
public final class ConfigSnapshot {

    private final Map<String, Map<String, String>> files;

    public ConfigSnapshot(Map<String, Map<String, String>> files) {
        this.files = files == null ? Map.of() : files;
    }

    public static ConfigSnapshot empty() { return new ConfigSnapshot(Map.of()); }

    public boolean isEmpty() {
        return files.values().stream().allMatch(Map::isEmpty);
    }

    public Optional<String> get(String file, String key) {
        Map<String, String> m = files.get(file);
        return m == null ? Optional.empty() : Optional.ofNullable(m.get(key));
    }
}
