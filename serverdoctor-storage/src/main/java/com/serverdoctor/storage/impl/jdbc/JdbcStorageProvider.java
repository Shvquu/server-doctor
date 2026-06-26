package com.serverdoctor.storage.impl.jdbc;

import com.serverdoctor.api.module.DiagnosticReport;
import com.serverdoctor.storage.StorageException;
import com.serverdoctor.storage.StorageProvider;
import com.serverdoctor.storage.node.InMemoryNodeRepository;
import com.serverdoctor.storage.repository.*;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.Statement;

/**
 * Gemeinsames Backend für PostgreSQL und MariaDB. Beide unterscheiden sich nur im
 * {@link JdbcDialect}; das Verbindungspooling übernimmt HikariCP, daher braucht es
 * keine manuelle Serialisierung wie bei SQLite – der Pool kümmert sich um
 * Nebenläufigkeit und Reconnect.
 *
 * <p>Benötigt zur Laufzeit {@code com.zaxxer:HikariCP} sowie den jeweiligen Treiber
 * ({@code org.postgresql:postgresql} bzw. {@code org.mariadb.jdbc:mariadb-java-client}).
 */
public final class JdbcStorageProvider implements StorageProvider {

    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final JdbcDialect dialect;

    private final NodeRepository nodes = new InMemoryNodeRepository();

    private HikariDataSource dataSource;
    private JdbcContext ctx;

    private PerformanceRepository performance;
    private ConflictRepository conflicts;
    private SecurityRepository security;
    private RecommendationRepository recommendations;
    private PluginRepository plugins;

    public JdbcStorageProvider(String jdbcUrl, String username, String password, JdbcDialect dialect) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
        this.dialect = dialect;
    }

    /** Convenience-Factory, damit StorageProviders keinen Dialekt-Import braucht. */
    public static JdbcStorageProvider postgres(String jdbcUrl, String username, String password) {
        return new JdbcStorageProvider(jdbcUrl, username, password, JdbcDialect.POSTGRES);
    }

    public static JdbcStorageProvider mariadb(String jdbcUrl, String username, String password) {
        return new JdbcStorageProvider(jdbcUrl, username, password, JdbcDialect.MARIADB);
    }

    @Override
    public void initialize() {
        try {
            // Expliziter Load gibt eine klare Fehlermeldung, falls der Treiber fehlt.
            Class.forName(dialect.driverClassName);
        } catch (ClassNotFoundException e) {
            throw new StorageException("JDBC-Treiber fehlt im Classpath: " + dialect.driverClassName, e);
        }
        try {
            HikariConfig hc = new HikariConfig();
            hc.setPoolName(dialect.poolName);
            hc.setJdbcUrl(jdbcUrl);
            hc.setUsername(username);
            hc.setPassword(password);
            hc.setDriverClassName(dialect.driverClassName);
            hc.setMaximumPoolSize(10);
            hc.setMinimumIdle(2);
            hc.setConnectionTimeout(30_000L);
            hc.setMaxLifetime(1_800_000L);
            this.dataSource = new HikariDataSource(hc);
            this.ctx = new JdbcContext(dataSource, dialect);

            try (Connection c = dataSource.getConnection(); Statement st = c.createStatement()) {
                for (String ddl : JdbcSchema.statements(dialect)) st.execute(ddl);
            }

            this.performance = new JdbcPerformanceRepository(ctx);
            this.conflicts = new JdbcConflictRepository(ctx);
            this.security = new JdbcSecurityRepository(ctx);
            this.recommendations = new JdbcRecommendationRepository(ctx);
            this.plugins = new JdbcPluginRepository(ctx);
        } catch (Exception e) {
            throw new StorageException(dialect + "-Initialisierung fehlgeschlagen: " + jdbcUrl, e);
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
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    @Override
    public void saveReport(DiagnosticReport report) {
        StorageProvider.super.saveReport(report);
    }
}
