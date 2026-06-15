package com.serverdoctor.storage.impl.jdbc;

/**
 * DDL für die SQL-Server-Backends. Spiegelt {@code SqliteSchema} wider.
 *
 * <p>Indizierte Zeitstempel-Spalten sind {@code VARCHAR(64)} statt {@code TEXT}:
 * MariaDB kann TEXT-Spalten nicht ohne Präfixlänge indizieren, ein ISO-8601-Instant
 * ist deutlich kürzer als 64 Zeichen. Postgres ist mit beidem zufrieden.
 */
final class JdbcSchema {

    private JdbcSchema() {}

    static String[] statements(JdbcDialect d) {
        String id = d.serialPrimaryKey;
        String dbl = d.doubleType;
        return new String[] {
            """
            CREATE TABLE IF NOT EXISTS performance (
                id %s,
                captured_at VARCHAR(64) NOT NULL,
                tps1m %s, tps5m %s, tps15m %s, mspt %s,
                mem_used BIGINT, mem_committed BIGINT, mem_max BIGINT,
                gc_count BIGINT, gc_time BIGINT,
                threads INTEGER, players INTEGER
            )
            """.formatted(id, dbl, dbl, dbl, dbl),
            "CREATE INDEX IF NOT EXISTS idx_performance_time ON performance(captured_at)",
            """
            CREATE TABLE IF NOT EXISTS conflicts (
                id %s,
                at VARCHAR(64) NOT NULL, conflict_id VARCHAR(255), plugin_a VARCHAR(255),
                plugin_b VARCHAR(255), severity VARCHAR(32), description TEXT
            )
            """.formatted(id),
            "CREATE INDEX IF NOT EXISTS idx_conflicts_time ON conflicts(at)",
            """
            CREATE TABLE IF NOT EXISTS security_risks (
                id %s,
                at VARCHAR(64) NOT NULL, plugin_name VARCHAR(255), type VARCHAR(64),
                severity VARCHAR(32), description TEXT
            )
            """.formatted(id),
            "CREATE INDEX IF NOT EXISTS idx_security_time ON security_risks(at)",
            """
            CREATE TABLE IF NOT EXISTS recommendations (
                id %s,
                at VARCHAR(64) NOT NULL, rec_id VARCHAR(255), category VARCHAR(64),
                severity VARCHAR(32), title VARCHAR(512), description TEXT
            )
            """.formatted(id),
            "CREATE INDEX IF NOT EXISTS idx_recommendations_time ON recommendations(at)",
            """
            CREATE TABLE IF NOT EXISTS plugin_inventory (
                id %s,
                at VARCHAR(64) NOT NULL, name VARCHAR(255), version VARCHAR(128),
                authors TEXT, enabled INTEGER
            )
            """.formatted(id),
            "CREATE INDEX IF NOT EXISTS idx_inventory_time ON plugin_inventory(at)"
        };
    }
}
