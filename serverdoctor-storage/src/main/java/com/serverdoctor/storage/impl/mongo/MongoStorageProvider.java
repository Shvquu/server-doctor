package com.serverdoctor.storage.impl.mongo;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.serverdoctor.api.module.DiagnosticReport;
import com.serverdoctor.storage.StorageException;
import com.serverdoctor.storage.StorageProvider;
import com.serverdoctor.storage.node.InMemoryNodeRepository;
import com.serverdoctor.storage.repository.*;

/**
 * MongoDB-Backend. Erwartet eine vollständige Connection-URI (inkl. ggf. Zugangsdaten,
 * {@code authSource} und Datenbankname), wie sie der Paper-Adapter aus der config.yml
 * zusammensetzt. Benötigt zur Laufzeit {@code org.mongodb:mongodb-driver-sync}.
 */
public final class MongoStorageProvider implements StorageProvider {

    private final String connectionString;
    private MongoContext ctx;

    private PerformanceRepository performance;
    private ConflictRepository conflicts;
    private SecurityRepository security;
    private RecommendationRepository recommendations;
    private PluginRepository plugins;
    private final NodeRepository nodes = new InMemoryNodeRepository();

    public MongoStorageProvider(String connectionString) {
        this.connectionString = connectionString;
    }

    @Override
    public void initialize() {
        try {
            ConnectionString cs = new ConnectionString(connectionString);
            MongoClientSettings settings = MongoClientSettings.builder()
                    .applicationName("ServerDoctor")
                    .applyConnectionString(cs)
                    .build();
            MongoClient client = MongoClients.create(settings);

            String dbName = cs.getDatabase() != null && !cs.getDatabase().isBlank()
                    ? cs.getDatabase() : "serverdoctor";
            MongoDatabase database = client.getDatabase(dbName);

            this.ctx = new MongoContext(client, database);
            this.performance = new MongoPerformanceRepository(ctx);
            this.conflicts = new MongoConflictRepository(ctx);
            this.security = new MongoSecurityRepository(ctx);
            this.recommendations = new MongoRecommendationRepository(ctx);
            this.plugins = new MongoPluginRepository(ctx);
        } catch (Exception e) {
            throw new StorageException("MongoDB-Initialisierung fehlgeschlagen", e);
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
        if (ctx != null) {
            try {
                ctx.client.close();
            } catch (Exception e) {
                throw new StorageException("Konnte MongoDB-Verbindung nicht schließen", e);
            }
        }
    }

    @Override
    public void saveReport(DiagnosticReport report) {
        StorageProvider.super.saveReport(report);
    }
}
