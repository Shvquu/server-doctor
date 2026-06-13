package com.serverdoctor.storage.impl.sqlite;

/** DDL für das SQLite-Backend. */
final class SqliteSchema {

    private SqliteSchema() {}

    static final String[] STATEMENTS = {
        """
        CREATE TABLE IF NOT EXISTS performance (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            captured_at TEXT NOT NULL,
            tps1m REAL, tps5m REAL, tps15m REAL, mspt REAL,
            mem_used INTEGER, mem_committed INTEGER, mem_max INTEGER,
            gc_count INTEGER, gc_time INTEGER,
            threads INTEGER, players INTEGER
        )
        """,
        "CREATE INDEX IF NOT EXISTS idx_performance_time ON performance(captured_at)",
        """
        CREATE TABLE IF NOT EXISTS conflicts (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            at TEXT NOT NULL, conflict_id TEXT, plugin_a TEXT, plugin_b TEXT,
            severity TEXT, description TEXT
        )
        """,
        "CREATE INDEX IF NOT EXISTS idx_conflicts_time ON conflicts(at)",
        """
        CREATE TABLE IF NOT EXISTS security_risks (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            at TEXT NOT NULL, plugin_name TEXT, type TEXT, severity TEXT, description TEXT
        )
        """,
        "CREATE INDEX IF NOT EXISTS idx_security_time ON security_risks(at)",
        """
        CREATE TABLE IF NOT EXISTS recommendations (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            at TEXT NOT NULL, rec_id TEXT, category TEXT, severity TEXT, title TEXT, description TEXT
        )
        """,
        "CREATE INDEX IF NOT EXISTS idx_recommendations_time ON recommendations(at)",
        """
        CREATE TABLE IF NOT EXISTS plugin_inventory (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            at TEXT NOT NULL, name TEXT, version TEXT, authors TEXT, enabled INTEGER
        )
        """,
        "CREATE INDEX IF NOT EXISTS idx_inventory_time ON plugin_inventory(at)"
    };
}
