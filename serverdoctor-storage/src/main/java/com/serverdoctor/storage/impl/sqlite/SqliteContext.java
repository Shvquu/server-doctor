package com.serverdoctor.storage.impl.sqlite;

import java.sql.Connection;

/** Hält die geteilte Connection plus Lock. SQLite-Zugriffe werden serialisiert. */
final class SqliteContext {
    final Connection connection;
    final Object lock = new Object();

    SqliteContext(Connection connection) { this.connection = connection; }
}
