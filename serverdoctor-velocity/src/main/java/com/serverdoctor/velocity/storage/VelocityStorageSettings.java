package com.serverdoctor.velocity.storage;

import com.serverdoctor.storage.StorageConfig;
import com.serverdoctor.storage.StorageType;
import org.yaml.snakeyaml.Yaml;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Velocity-Pendant zum Paper-{@code StorageSettings}. Velocity hat keine Bukkit-
 * Konfiguration, daher wird die config.yml hier mit SnakeYAML in eine verschachtelte
 * {@code Map} geparst. Die Logik zum Bauen der {@link StorageConfig} (JDBC-URLs,
 * Mongo-URI, URL-Encoding, Defaults) ist identisch zur Paper-Seite.
 *
 * <p>Dies ist die einzige Stelle, an der Velocity die Persistenz berührt – das
 * Storage-Modul bleibt frei von Plattform-Code (ArchUnit-konform).
 */
public final class VelocityStorageSettings {

    private VelocityStorageSettings() {}

    /**
     * Kopiert die mitgelieferte config.yml ins Datenverzeichnis (falls nicht vorhanden),
     * liest sie ein und baut die {@link StorageConfig}.
     *
     * @param dataDirectory das {@code @DataDirectory} des Plugins
     * @param bundledDefault {@code getClass().getResourceAsStream("/config.yml")}
     */
    public static StorageConfig load(Path dataDirectory, InputStream bundledDefault) throws Exception {
        Files.createDirectories(dataDirectory);
        Path file = dataDirectory.resolve("config.yml");
        if (!Files.exists(file) && bundledDefault != null) {
            Files.copy(bundledDefault, file);
        }

        Map<String, Object> root;
        try (InputStream in = Files.newInputStream(file)) {
            Object loaded = new Yaml().load(in);
            root = loaded instanceof Map ? cast(loaded) : Collections.emptyMap();
        }
        return from(root, dataDirectory);
    }

    static StorageConfig from(Map<String, Object> root, Path dataDirectory) {
        Map<String, Object> storage = section(root, "storage");
        String typeRaw = str(storage, "type", "sqlite").trim().toLowerCase();

        return switch (typeRaw) {
            case "memory", "in-memory", "inmemory" -> StorageConfig.memory();

            case "sqlite" -> {
                Map<String, Object> c = section(storage, "sqlite");
                String fileName = str(c, "file", "serverdoctor.db");
                Path p = Path.of(fileName);
                if (!p.isAbsolute()) p = dataDirectory.resolve(fileName);
                yield StorageConfig.sqlite(p.toString());
            }

            case "postgres", "postgresql" -> {
                Map<String, Object> c = require(storage, "postgresql");
                String url = "jdbc:postgresql://" + str(c, "host", "localhost")
                        + ":" + integer(c, "port", 5432)
                        + "/" + str(c, "database", "serverdoctor")
                        + queryString(section(c, "properties"));
                yield new StorageConfig(StorageType.POSTGRES, url,
                        str(c, "username", ""), str(c, "password", ""));
            }

            case "mariadb", "mysql" -> {
                Map<String, Object> c = require(storage, "mariadb");
                String url = "jdbc:mariadb://" + str(c, "host", "localhost")
                        + ":" + integer(c, "port", 3306)
                        + "/" + str(c, "database", "serverdoctor")
                        + queryString(section(c, "properties"));
                yield new StorageConfig(StorageType.MARIADB, url,
                        str(c, "username", ""), str(c, "password", ""));
            }

            case "mongo", "mongodb" -> {
                Map<String, Object> c = require(storage, "mongodb");
                yield new StorageConfig(StorageType.MONGODB, mongoUri(c), null, null);
            }

            default -> throw new IllegalArgumentException("Unbekannter storage.type: " + typeRaw);
        };
    }

    private static String mongoUri(Map<String, Object> c) {
        String explicit = str(c, "connection-string", "");
        if (!explicit.isBlank()) return explicit;

        String user = str(c, "username", "");
        String credentials = user.isBlank() ? "" : enc(user) + ":" + enc(str(c, "password", "")) + "@";
        String authSource = str(c, "auth-database", "admin");
        return "mongodb://" + credentials
                + str(c, "host", "localhost") + ":" + integer(c, "port", 27017)
                + "/" + str(c, "database", "serverdoctor")
                + "?authSource=" + enc(authSource);
    }

    private static String queryString(Map<String, Object> props) {
        if (props.isEmpty()) return "";
        StringJoiner sj = new StringJoiner("&", "?", "");
        sj.setEmptyValue("");
        for (Map.Entry<String, Object> e : props.entrySet()) {
            sj.add(enc(e.getKey()) + "=" + enc(String.valueOf(e.getValue())));
        }
        return sj.toString();
    }

    // ---- kleine, null-sichere Map-Helfer ------------------------------------

    @SuppressWarnings("unchecked")
    private static Map<String, Object> cast(Object o) {
        return (Map<String, Object>) o;
    }

    private static Map<String, Object> section(Map<String, Object> parent, String key) {
        Object v = parent == null ? null : parent.get(key);
        return v instanceof Map ? cast(v) : Collections.emptyMap();
    }

    private static Map<String, Object> require(Map<String, Object> parent, String key) {
        Object v = parent == null ? null : parent.get(key);
        if (!(v instanceof Map)) {
            throw new IllegalStateException("Fehlende Sektion 'storage." + key + "' in config.yml");
        }
        return cast(v);
    }

    private static String str(Map<String, Object> m, String key, String def) {
        Object v = m.get(key);
        return v == null ? def : String.valueOf(v);
    }

    private static int integer(Map<String, Object> m, String key, int def) {
        Object v = m.get(key);
        if (v instanceof Number n) return n.intValue();
        if (v != null) {
            try { return Integer.parseInt(v.toString().trim()); } catch (NumberFormatException ignored) { }
        }
        return def;
    }

    private static String enc(String v) {
        return URLEncoder.encode(v == null ? "" : v, StandardCharsets.UTF_8);
    }
}
