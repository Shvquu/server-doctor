package com.serverdoctor.storage;

import com.serverdoctor.storage.impl.jdbc.JdbcStorageProvider;
import com.serverdoctor.storage.impl.memory.MemoryStorageProvider;
import com.serverdoctor.storage.impl.mongo.MongoStorageProvider;
import com.serverdoctor.storage.impl.sqlite.SqliteStorageProvider;

/** Erstellt einen StorageProvider passend zur Konfiguration. */
public final class StorageProviders {

    private StorageProviders() {}

    public static StorageProvider create(StorageConfig config) {
        return switch (config.type()) {
            case MEMORY   -> new MemoryStorageProvider();
            case SQLITE   -> new SqliteStorageProvider(config.location());
            case POSTGRES -> JdbcStorageProvider.postgres(config.location(), config.username(), config.password());
            case MARIADB  -> JdbcStorageProvider.mariadb(config.location(), config.username(), config.password());
            case MONGODB  -> new MongoStorageProvider(config.location());
        };
    }
}
