package com.serverdoctor.bungee.service;

import com.serverdoctor.common.model.Severity;
import com.serverdoctor.rest.RestApiConfig;
import com.serverdoctor.webhook.WebhookConfig;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * BungeeCord has no Bukkit config, so config.yml is parsed with SnakeYAML into a nested map and
 * mapped to the framework-free {@link RestApiConfig} / {@link WebhookConfig}. Mirrors the
 * Velocity service reader.
 */
public final class BungeeServiceSettings {

    private BungeeServiceSettings() {}

    /** Parses {@code <dataFolder>/config.yml}; returns an empty map if absent. */
    public static Map<String, Object> load(File dataFolder) throws Exception {
        File file = new File(dataFolder, "config.yml");
        if (!file.exists()) return Collections.emptyMap();
        try (InputStream in = Files.newInputStream(file.toPath())) {
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

    /** Reads the security.advisory block; helper kept here so all config parsing lives together. */
    public static AdvisorySettings advisory(Map<String, Object> root) {
        Map<String, Object> sec = section(root, "security");
        Map<String, Object> adv = section(sec, "advisory");
        if (adv.isEmpty()) return new AdvisorySettings(false, "", 360L);
        return new AdvisorySettings(
                bool(adv, "enabled", false),
                str(adv, "feed-url", ""),
                integer(adv, "refresh-minutes", 360));
    }

    public record AdvisorySettings(boolean enabled, String feedUrl, long refreshMinutes) {}
}
