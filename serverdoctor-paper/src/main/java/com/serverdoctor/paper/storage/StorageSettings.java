package com.serverdoctor.paper.storage;
import com.serverdoctor.common.exception.ConfigurationException;

import com.serverdoctor.storage.StorageConfig;
import com.serverdoctor.storage.StorageType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Liest die {@code storage}-Sektion der config.yml und baut daraus eine
 * plattformfreie {@link StorageConfig}. Dies ist die EINZIGE Stelle, an der Bukkit-
 * Konfiguration die Persistenzschicht berührt – das Storage-Modul bleibt damit frei
 * von Plattform-SDK (ArchUnit-konform).
 *
 * <p>Für die SQL-Server wird {@code location} als JDBC-URL gesetzt, Benutzer/Passwort
 * separat. Für MongoDB wird eine vollständige Connection-URI (inkl. Zugangsdaten und
 * {@code authSource}) in {@code location} kodiert.
 */
public final class StorageSettings {

    private StorageSettings() {}

    public static StorageConfig from(FileConfiguration root, File dataFolder) {
        ConfigurationSection s = root.getConfigurationSection("storage");
        if (s == null) {
            return StorageConfig.sqlite(new File(dataFolder, "serverdoctor.db").getAbsolutePath());
        }

        String typeRaw = s.getString("type", "sqlite").trim().toLowerCase();
        return switch (typeRaw) {
            case "memory", "in-memory", "inmemory" -> StorageConfig.memory();

            case "sqlite" -> {
                String file = s.getString("sqlite.file", "data/serverdoctor.db");
                File f = new File(file);
                if (!f.isAbsolute()) f = new File(dataFolder, file);
                yield StorageConfig.sqlite(f.getAbsolutePath());
            }

            case "postgres", "postgresql" -> {
                ConfigurationSection c = require(s, "postgresql");
                String url = "jdbc:postgresql://" + c.getString("host", "localhost")
                        + ":" + c.getInt("port", 5432)
                        + "/" + c.getString("database", "serverdoctor")
                        + queryString(c.getConfigurationSection("properties"));
                yield new StorageConfig(StorageType.POSTGRES, url,
                        c.getString("username", ""), c.getString("password", ""));
            }

            case "mariadb", "mysql" -> {
                ConfigurationSection c = require(s, "mariadb");
                String url = "jdbc:mariadb://" + c.getString("host", "localhost")
                        + ":" + c.getInt("port", 3306)
                        + "/" + c.getString("database", "serverdoctor")
                        + queryString(c.getConfigurationSection("properties"));
                yield new StorageConfig(StorageType.MARIADB, url,
                        c.getString("username", ""), c.getString("password", ""));
            }

            case "mongo", "mongodb" -> {
                ConfigurationSection c = require(s, "mongodb");
                yield new StorageConfig(StorageType.MONGODB, mongoUri(c), null, null);
            }

            default -> throw new ConfigurationException("Unbekannter storage.type: " + typeRaw);
        };
    }

    private static String mongoUri(ConfigurationSection c) {
        String explicit = c.getString("connection-string", "");
        if (explicit != null && !explicit.isBlank()) return explicit;

        String user = c.getString("username", "");
        String pass = c.getString("password", "");
        String credentials = "";
        if (user != null && !user.isBlank()) {
            credentials = enc(user) + ":" + enc(pass) + "@";
        }
        String authSource = c.getString("auth-database", "admin");
        return "mongodb://" + credentials
                + c.getString("host", "localhost") + ":" + c.getInt("port", 27017)
                + "/" + c.getString("database", "serverdoctor")
                + "?authSource=" + enc(authSource);
    }

    /** Baut {@code ?k=v&k2=v2} aus einem optionalen properties-Abschnitt. */
    private static String queryString(ConfigurationSection props) {
        if (props == null) return "";
        StringJoiner sj = new StringJoiner("&", "?", "");
        sj.setEmptyValue("");
        for (Map.Entry<String, Object> e : props.getValues(false).entrySet()) {
            sj.add(enc(e.getKey()) + "=" + enc(String.valueOf(e.getValue())));
        }
        return sj.toString();
    }

    private static ConfigurationSection require(ConfigurationSection parent, String key) {
        ConfigurationSection c = parent.getConfigurationSection(key);
        if (c == null) {
            throw new ConfigurationException("Fehlende Sektion 'storage." + key + "' in config.yml");
        }
        return c;
    }

    private static String enc(String v) {
        return URLEncoder.encode(v == null ? "" : v, StandardCharsets.UTF_8);
    }
}
