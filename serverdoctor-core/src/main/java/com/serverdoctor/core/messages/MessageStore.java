package com.serverdoctor.core.messages;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Plattformneutraler Nachrichtenspeicher. Lädt einfache, flache YAML-Dateien
 * der Form {@code schluessel: "Text mit {platzhalter}"}.
 *
 * Defaults stammen aus der mitgelieferten messages.yml; eine vom Nutzer
 * editierte Datei wird darübergelegt. Fehlende Schlüssel fallen auf den
 * Default zurück, unbekannte Schlüssel liefern den Schlüssel selbst.
 *
 * Farbcodes ({@code &x}) und {@code {platzhalter}} bleiben unangetastet -
 * die Plattform entscheidet, wie sie gerendert werden.
 */
public class MessageStore {

    private final Map<String, String> defaults = new HashMap<>();
    private final Map<String, String> overrides = new HashMap<>();

    public void loadDefaults(InputStream in) { parseInto(read(in), defaults); }
    public void loadDefaults(String text) { parseInto(text, defaults); }
    public void applyOverrides(InputStream in) { parseInto(read(in), overrides); }
    public void applyOverrides(String text) { parseInto(text, overrides); }

    /** Verwirft die geladenen Overrides (für /serverdoctor reload). Defaults bleiben erhalten. */
    public void clearOverrides() { overrides.clear(); }

    /** Rohtext (mit &-Codes und {Platzhaltern}); override > default > Schlüssel. */
    public String raw(String key) {
        String v = overrides.get(key);
        if (v != null) return v;
        v = defaults.get(key);
        return v != null ? v : key;
    }

    /** Wie {@link #raw(String)}, aber ersetzt {@code {name}} aus key/value-Paaren. */
    public String get(String key, Object... placeholders) {
        String s = raw(key);
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            s = s.replace("{" + placeholders[i] + "}", String.valueOf(placeholders[i + 1]));
        }
        return s;
    }

    private static void parseInto(String text, Map<String, String> target) {
        if (text == null) return;
        for (String line : text.split("\r?\n")) {
            String trimmed = line.strip();
            if (trimmed.isEmpty() || trimmed.charAt(0) == '#') continue;
            int sep = trimmed.indexOf(':');
            if (sep < 0) continue;
            String key = trimmed.substring(0, sep).strip();
            String value = unquote(trimmed.substring(sep + 1).strip());
            if (!key.isEmpty()) target.put(key, value);
        }
    }

    private static String unquote(String v) {
        if (v.length() >= 2) {
            char first = v.charAt(0);
            char last = v.charAt(v.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return v.substring(1, v.length() - 1);
            }
        }
        return v;
    }

    private static String read(InputStream in) {
        if (in == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) sb.append(line).append('\n');
        } catch (IOException e) {
            return sb.toString();
        }
        return sb.toString();
    }

}
