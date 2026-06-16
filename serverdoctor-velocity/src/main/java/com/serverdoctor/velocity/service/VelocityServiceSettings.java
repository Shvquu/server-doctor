package com.serverdoctor.velocity.service;

import com.serverdoctor.common.model.Severity;
import com.serverdoctor.rest.RestApiConfig;
import com.serverdoctor.webhook.WebhookConfig;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Velocity has no Bukkit config, so config.yml is parsed with SnakeYAML into a nested map and
 * mapped to the framework-free {@link RestApiConfig} / {@link WebhookConfig}.
 */
public final class VelocityServiceSettings {

    private VelocityServiceSettings() {}

    /** Loads and parses {@code <dataDir>/config.yml}; returns an empty map if absent. */
    public static Map<String, Object> load(Path dataDirectory) throws Exception {
        Path file = dataDirectory.resolve("config.yml");
        if (!Files.exists(file)) return Collections.emptyMap();
        try (InputStream in = Files.newInputStream(file)) {
            Object root = new Yaml().load(in);
            return root instanceof Map ? cast(root) : Collections.emptyMap();
        }
    }

    public static RestApiConfig restApi(Map<String, Object> root) {
        Map<String, Object> s = section(root, "rest-api");
        if (s.isEmpty()) return RestApiConfig.disabled();
        String token = str(s, "token", "");
        return new RestApiConfig(
                bool(s, "enabled", false),
                str(s, "host", "127.0.0.1"),
                integer(s, "port", 9173),
                token.isBlank() ? null : token);
    }

    public static WebhookConfig webhooks(Map<String, Object> root) {
        Map<String, Object> s = section(root, "webhooks");
        if (s.isEmpty()) return WebhookConfig.disabled();

        Severity min = parseSeverity(str(s, "min-severity", "HIGH"));
        List<WebhookConfig.Target> targets = new ArrayList<>();
        Object raw = s.get("targets");
        if (raw instanceof List<?> list) {
            for (Object o : list) {
                if (!(o instanceof Map<?, ?> m)) continue;
                Object url = m.get("url");
                if (url == null || url.toString().isBlank()) continue;
                Object name = m.get("name");
                try {
                    targets.add(new WebhookConfig.Target(
                            WebhookConfig.Type.from(String.valueOf(m.get("type"))),
                            url.toString(),
                            name == null ? null : name.toString()));
                } catch (IllegalArgumentException ignored) {
                    // unknown webhook type -> skip
                }
            }
        }
        return new WebhookConfig(bool(s, "enabled", false), min, targets);
    }

    private static Severity parseSeverity(String raw) {
        try {
            return Severity.valueOf(raw.trim().toUpperCase());
        } catch (Exception e) {
            return Severity.HIGH;
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> cast(Object o) { return (Map<String, Object>) o; }

    private static Map<String, Object> section(Map<String, Object> parent, String key) {
        Object v = parent == null ? null : parent.get(key);
        return v instanceof Map ? cast(v) : Collections.emptyMap();
    }

    private static String str(Map<String, Object> m, String key, String def) {
        Object v = m.get(key);
        return v == null ? def : String.valueOf(v);
    }

    private static boolean bool(Map<String, Object> m, String key, boolean def) {
        Object v = m.get(key);
        if (v instanceof Boolean b) return b;
        if (v != null) return Boolean.parseBoolean(v.toString().trim());
        return def;
    }

    private static int integer(Map<String, Object> m, String key, int def) {
        Object v = m.get(key);
        if (v instanceof Number num) return num.intValue();
        if (v != null) {
            try { return Integer.parseInt(v.toString().trim()); } catch (NumberFormatException ignored) { }
        }
        return def;
    }
}
