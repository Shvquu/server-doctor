package com.serverdoctor.core.config;

import org.yaml.snakeyaml.Yaml;
import java.io.File;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
 * Reads the well-known server config files from the working directory and flattens them into a
 * neutral {@link ConfigSnapshot}. Pure JDK + SnakeYAML, so the same reader works on Paper/Folia,
 * Velocity and BungeeCord - each platform just reads whatever files exist in its directory.
 *
 * <p>Logical file names: {@code server.properties}, {@code bukkit.yml}, {@code spigot.yml},
 * {@code paper-global.yml}, {@code paper-world.yml}, {@code velocity.toml}.
 */
public class FilesystemConfigSource implements ConfigSource{

    private static final Map<String, String[]> FILES = new LinkedHashMap<>();
    static {
        FILES.put("server.properties", new String[]{"server.properties"});
        FILES.put("bukkit.yml",        new String[]{"bukkit.yml"});
        FILES.put("spigot.yml",        new String[]{"spigot.yml"});
        FILES.put("paper-global.yml",  new String[]{"config/paper-global.yml", "paper-global.yml"});
        FILES.put("paper-world.yml",   new String[]{"config/paper-world-defaults.yml", "paper-world.yml"});
        FILES.put("velocity.toml",     new String[]{"velocity.toml"});
    }

    private final File baseDir;

    public FilesystemConfigSource() {
        this(new File(System.getProperty("user.dir", ".")));
    }

    public FilesystemConfigSource(File baseDir) {
        this.baseDir = baseDir;
    }

    @Override
    public ConfigSnapshot read() {
        Map<String, Map<String, String>> out = new HashMap<>();
        for (Map.Entry<String, String[]> e : FILES.entrySet()) {
            File file = firstExisting(e.getValue());
            if (file == null) continue;
            try {
                String text = Files.readString(file.toPath(), StandardCharsets.UTF_8);
                Map<String, String> flat = e.getKey().endsWith(".properties") ? parseProperties(text)
                        : e.getKey().endsWith(".toml") ? parseToml(text)
                          : flattenYaml(text);
                if (!flat.isEmpty()) out.put(e.getKey(), flat);
            } catch (Exception ignored) {
                // unreadable/parse error -> just skip this file
            }
        }
        return new ConfigSnapshot(out);
    }

    private File firstExisting(String[] candidates) {
        for (String c : candidates) {
            File f = new File(baseDir, c);
            if (f.isFile()) return f;
        }
        return null;
    }

    // ---- parsers (package-private for tests) --------------------------------

    static Map<String, String> parseProperties(String text) throws Exception {
        Properties p = new Properties();
        p.load(new StringReader(text));
        Map<String, String> out = new HashMap<>();
        for (String name : p.stringPropertyNames()) out.put(name, p.getProperty(name));
        return out;
    }

    @SuppressWarnings("unchecked")
    static Map<String, String> flattenYaml(String text) {
        Object root = new Yaml().load(text);
        Map<String, String> out = new HashMap<>();
        if (root instanceof Map) flatten("", (Map<String, Object>) root, out);
        return out;
    }

    @SuppressWarnings("unchecked")
    static void flatten(String prefix, Map<String, Object> map, Map<String, String> out) {
        for (Map.Entry<String, Object> e : map.entrySet()) {
            String key = prefix.isEmpty() ? e.getKey() : prefix + "." + e.getKey();
            Object v = e.getValue();
            if (v instanceof Map) {
                flatten(key, (Map<String, Object>) v, out);
            } else if (v instanceof List) {
                out.put(key, String.valueOf(v));
            } else if (v != null) {
                out.put(key, String.valueOf(v));
            }
        }
    }

    /** Minimal TOML reader: top-level + one-level [section] flat keys, enough for velocity.toml. */
    static Map<String, String> parseToml(String text) {
        Map<String, String> out = new HashMap<>();
        String section = "";
        for (String raw : text.split("\\r?\\n")) {
            String line = stripComment(raw).trim();
            if (line.isEmpty()) continue;
            if (line.startsWith("[") && line.endsWith("]")) {
                section = line.substring(1, line.length() - 1).trim().toLowerCase(Locale.ROOT);
                continue;
            }
            int eq = line.indexOf('=');
            if (eq <= 0) continue;
            String key = line.substring(0, eq).trim();
            String value = unquote(line.substring(eq + 1).trim());
            String full = section.isEmpty() ? key : section + "." + key;
            out.put(full, value);
        }
        return out;
    }

    private static String stripComment(String line) {
        boolean inString = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') inString = !inString;
            else if (c == '#' && !inString) return line.substring(0, i);
        }
        return line;
    }

    private static String unquote(String v) {
        if (v.length() >= 2 && ((v.startsWith("\"") && v.endsWith("\"")) || (v.startsWith("'") && v.endsWith("'")))) {
            return v.substring(1, v.length() - 1);
        }
        return v;
    }
}