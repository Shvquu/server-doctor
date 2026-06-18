package com.serverdoctor.bungee.service;

import com.serverdoctor.common.model.Severity;
import com.serverdoctor.rest.RestApiConfig;
import com.serverdoctor.webhook.WebhookConfig;
import net.md_5.bungee.config.Configuration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Reads the rest-api and webhooks sections of config.yml into framework-free
 * config records. The only place Bungee config touches these modules.
 */
public final class BungeeServiceSettings {

    private BungeeServiceSettings() {
    }

    public static RestApiConfig restApi(Configuration cfg) {
        Configuration s = cfg.getSection("rest-api");
        if (s == null) {
            return RestApiConfig.disabled();
        }

        String token = s.getString("token", "");

        return new RestApiConfig(
                s.getBoolean("enabled", false),
                s.getString("host", "127.0.0.1"),
                s.getInt("port", 9173),
                token.isBlank() ? null : token
        );
    }

    public static WebhookConfig webhooks(Configuration cfg) {
        Configuration s = cfg.getSection("webhooks");
        if (s == null) {
            return WebhookConfig.disabled();
        }

        Severity min = parseSeverity(s.getString("min-severity", "HIGH"));

        List<WebhookConfig.Target> targets = new ArrayList<>();

        Configuration targetsSection = s.getSection("targets");
        if (targetsSection != null) {
            Collection<String> keys = targetsSection.getKeys();

            for (String key : keys) {
                Configuration target = targetsSection.getSection(key);

                String url = target.getString("url");
                if (url == null || url.isBlank()) {
                    continue;
                }

                String name = target.getString("name", null);

                try {
                    targets.add(new WebhookConfig.Target(
                            WebhookConfig.Type.from(target.getString("type")),
                            url,
                            name
                    ));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        return new WebhookConfig(
                s.getBoolean("enabled", false),
                min,
                targets
        );
    }

    private static Severity parseSeverity(String raw) {
        try {
            return Severity.valueOf(raw.trim().toUpperCase());
        } catch (Exception e) {
            return Severity.HIGH;
        }
    }
}