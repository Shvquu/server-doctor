package com.serverdoctor.webhook;

import com.serverdoctor.common.model.Severity;

import java.util.List;

/** Framework-freie Webhook-Konfiguration. */
public record WebhookConfig(boolean enabled, Severity minSeverity, List<Target> targets) {

    public WebhookConfig {
        minSeverity = minSeverity == null ? Severity.HIGH : minSeverity;
        targets = targets == null ? List.of() : List.copyOf(targets);
    }

    public static WebhookConfig disabled() {
        return new WebhookConfig(false, Severity.HIGH, List.of());
    }

    public enum Type { DISCORD, SLACK, TEAMS;
        public static Type from(String raw) {
            return switch (raw == null ? "" : raw.trim().toLowerCase()) {
                case "discord" -> DISCORD;
                case "slack" -> SLACK;
                case "teams" -> TEAMS;
                default -> null;
            };
        }
    }

    /** Ein konfiguriertes Webhook-Ziel. */
    public record Target(WebhookConfig.Type type, String url, String name) {
        public boolean isValid() {
            return url != null && !url.isBlank();
        }
    }
}
