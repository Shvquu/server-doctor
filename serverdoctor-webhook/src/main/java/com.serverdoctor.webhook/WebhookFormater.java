package com.serverdoctor.webhook;

import com.serverdoctor.common.model.Severity;

import java.util.List;

record Notification(Severity severity, String title, String summary, List<String> details) {

    String hexColor() {
        return switch (severity) {
            case CRITICAL -> "B91C1C";
            case HIGH     -> "EA580C";
            case MEDIUM   -> "CA8A04";
            case LOW      -> "2563EB";
            case INFO     -> "0891B2";
            case OK       -> "16A34A";
        };
    }

    int decimalColor() {
        return Integer.parseInt(hexColor(), 16);
    }
}

interface WebhookFormater {
    String body(Notification n);

    static WebhookFormater forType(WebhookConfig.Type type) {
        return switch (type) {
            case DISCORD -> new Discord();
            case SLACK -> new Slack();
            case TEAMS -> new Teams();
        };
    }

    final class Discord implements WebhookFormater {
        @Override
        public String body(Notification n) {
            StringBuilder desc = new StringBuilder(n.summary());
            for (String line : n.details()) desc.append("\n• ").append(line);
            String embed = J.obj(
                    "title", J.s(n.title()),
                    "description", J.s(desc.toString()),
                    "color", Integer.toString(n.decimalColor()));
            return J.obj(
                    "username", J.s("ServerDoctor"),
                    "embeds", J.arr(List.of(embed)));
        }
    }

    final class Slack implements WebhookFormater {
        @Override
        public String body(Notification n) {
            StringBuilder text = new StringBuilder(n.summary());
            for (String line : n.details()) text.append("\n• ").append(line);
            String attachment = J.obj(
                    "color", J.s("#" + n.hexColor()),
                    "title", J.s(n.title()),
                    "text", J.s(text.toString()));
            return J.obj(
                    "text", J.s(n.title()),
                    "attachments", J.arr(List.of(attachment)));
        }
    }

    final class Teams implements WebhookFormater {
        @Override
        public String body(Notification n) {
            StringBuilder text = new StringBuilder(n.summary());
            for (String line : n.details()) text.append("  \n- ").append(line);
            return J.obj(
                    "@type", J.s("MessageCard"),
                    "@context", J.s("http://schema.org/extensions"),
                    "themeColor", J.s(n.hexColor()),
                    "summary", J.s(n.title()),
                    "title", J.s(n.title()),
                    "text", J.s(text.toString()));
        }
    }
}
