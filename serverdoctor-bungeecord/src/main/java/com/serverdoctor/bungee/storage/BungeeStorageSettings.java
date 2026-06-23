package com.serverdoctor.bungee.storage;

import com.serverdoctor.common.exception.ConfigurationException;

import com.serverdoctor.storage.StorageConfig;
import com.serverdoctor.storage.StorageType;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Map;
import java.util.StringJoiner;

/**
 * BungeeCord has no Bukkit config, so config.yml is parsed with SnakeYAML into a nested map and
 * mapped to a framework-free {@link StorageConfig}. Mirrors the Velocity storage reader.
 */
public final class BungeeStorageSettings {

    private BungeeStorageSettings() {}

    /** Copies the bundled config.yml into the data folder if absent, then parses + builds. */
    public static StorageConfig load(File dataFolder, InputStream bundledDefault) throws Exception {
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            throw new ConfigurationException("Could not create data folder: " + dataFolder);
        }
        File file = new File(dataFolder, "config.yml");
        if (!file.exists() && bundledDefault != null) {
            Files.copy(bundledDefault, file.toPath());
        }
        Map<String, Object> root;
        try (InputStream in = Files.newInputStream(file.toPath())) {
            Object loaded = new Yaml().load(in);
            root = loaded instanceof Map ? cast(loaded) : Collections.emptyMap();
        }
        return from(root, dataFolder);
    }

    static StorageConfig from(Map<String, Object> root, File dataFolder) {
        Map<String, Object> storage = section(root, "storage");
        String type = str(storage, "type", "sqlite").trim().toLowerCase();

        return switch (type) {
            case "memory", "in-memory", "inmemory" -> StorageConfig.memory();

            case "sqlite" -> {
                Map<String, Object> c = section(storage, "sqlite");
                String fileName = str(c, "file", "serverdoctor.db");
                File f = new File(fileName);
                if (!f.isAbsolute()) f = new File(dataFolder, fileName);
                yield StorageConfig.sqlite(f.getAbsolutePath());
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

            default -> throw new ConfigurationException("Unknown storage.type: " + type);
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

    @SuppressWarnings("unchecked")
    private static Map<String, Object> cast(Object o) { return (Map<String, Object>) o; }

    private static Map<String, Object> section(Map<String, Object> parent, String key) {
        Object v = parent == null ? null : parent.get(key);
        return v instanceof Map ? cast(v) : Collections.emptyMap();
    }

    private static Map<String, Object> require(Map<String, Object> parent, String key) {
        Object v = parent == null ? null : parent.get(key);
        if (!(v instanceof Map)) throw new ConfigurationException("Missing 'storage." + key + "' in config.yml");
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
