package com.serverdoctor.paper.service;
import com.serverdoctor.common.exception.ConfigurationException;

import com.serverdoctor.common.model.Severity;
import com.serverdoctor.rest.RestApiConfig;
import com.serverdoctor.webhook.WebhookConfig;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Reads the {@code rest-api} and {@code webhooks} sections of config.yml into framework-free
 * config records. The only place Bukkit config touches these modules.
 */
public final class PaperServiceSettings {

    private PaperServiceSettings() {}

    public static RestApiConfig restApi(FileConfiguration cfg) {
        ConfigurationSection s = cfg.getConfigurationSection("rest-api");
        if (s == null) return RestApiConfig.disabled();
        String token = s.getString("token", "");
        return new RestApiConfig(
                s.getBoolean("enabled", false),
                s.getString("host", "127.0.0.1"),
                s.getInt("port", 9173),
                token == null || token.isBlank() ? null : token);
    }

    public static WebhookConfig webhooks(FileConfiguration cfg) {
        ConfigurationSection s = cfg.getConfigurationSection("webhooks");
        if (s == null) return WebhookConfig.disabled();

        Severity min = parseSeverity(s.getString("min-severity", "HIGH"));
        List<WebhookConfig.Target> targets = new ArrayList<>();
        for (Map<?, ?> m : cfg.getMapList("webhooks.targets")) {
            Object url = m.get("url");
            if (url == null || url.toString().isBlank()) continue;
            Object name = m.get("name");
            try {
                targets.add(new WebhookConfig.Target(
                        WebhookConfig.Type.from(String.valueOf(m.get("type"))),
                        url.toString(),
                        name == null ? null : name.toString()));
            } catch (ConfigurationException ignored) {
                // unknown webhook type -> skip this target
            }
        }
        return new WebhookConfig(s.getBoolean("enabled", false), min, targets);
    }

    private static Severity parseSeverity(String raw) {
        try {
            return Severity.valueOf(raw.trim().toUpperCase());
        } catch (Exception e) {
            return Severity.HIGH;
        }
    }
}
