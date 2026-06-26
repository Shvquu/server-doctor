package com.serverdoctor.storage.impl.sqlite;

import com.serverdoctor.api.module.DiagnosticReport;
import com.serverdoctor.storage.StorageException;
import com.serverdoctor.storage.StorageProvider;
import com.serverdoctor.storage.node.InMemoryNodeRepository;
import com.serverdoctor.storage.repository.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/**
 * SQLite-Backend. Benötigt den Treiber {@code org.xerial:sqlite-jdbc} zur Laufzeit.
 * Zugriffe werden über {@link SqliteContext} serialisiert (ein Writer-Modell).
 */
public final class SqliteStorageProvider implements StorageProvider {

    private final String file;
    private SqliteContext ctx;

    private PerformanceRepository performance;
    private ConflictRepository conflicts;
    private SecurityRepository security;
    private RecommendationRepository recommendations;
    private PluginRepository plugins;
    private final NodeRepository nodes = new InMemoryNodeRepository();

    public SqliteStorageProvider(String file) {
        this.file = file == null ? "serverdoctor.db" : file;
    }

    @Override
    public void initialize() {
        try {
            // Expliziter Load gibt eine klare Fehlermeldung, falls der Treiber fehlt.
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new StorageException("SQLite-Treiber (org.xerial:sqlite-jdbc) fehlt im Classpath.", e);
        }
        try {
            Connection connection = DriverManager.getConnection("jdbc:sqlite:" + file);
            this.ctx = new SqliteContext(connection);
            try (Statement st = connection.createStatement()) {
                for (String ddl : SqliteSchema.STATEMENTS) st.execute(ddl);
            }
            this.performance = new SqlitePerformanceRepository(ctx);
            this.conflicts = new SqliteConflictRepository(ctx);
            this.security = new SqliteSecurityRepository(ctx);
            this.recommendations = new SqliteRecommendationRepository(ctx);
            this.plugins = new SqlitePluginRepository(ctx);
        } catch (Exception e) {
            throw new StorageException("SQLite-Initialisierung fehlgeschlagen: " + file, e);
        }
    }

    private void requireInit() {
        if (ctx == null) throw new StorageException("StorageProvider wurde nicht initialisiert.");
    }

    @Override public PerformanceRepository performance() { requireInit(); return performance; }
    @Override public ConflictRepository conflicts() { requireInit(); return conflicts; }
    @Override public SecurityRepository security() { requireInit(); return security; }
    @Override public RecommendationRepository recommendations() { requireInit(); return recommendations; }
    @Override public PluginRepository plugins() { requireInit(); return plugins; }
    @Override public NodeRepository nodes() { requireInit(); return nodes; }

    @Override
    public void close() {
        if (ctx == null) return;
        synchronized (ctx.lock) {
            try {
                ctx.connection.close();
            } catch (Exception e) {
                throw new StorageException("Konnte SQLite-Verbindung nicht schließen", e);
            }
        }
    }

    @Override
    public void saveReport(DiagnosticReport report) {
        StorageProvider.super.saveReport(report);
    }
}
