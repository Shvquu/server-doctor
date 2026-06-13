package com.serverdoctor.storage;

/** Konfiguration des Storage-Backends. */
public record StorageConfig(StorageType type, String location, String username, String password) {

    public static StorageConfig memory() {
        return new StorageConfig(StorageType.MEMORY, null, null, null);
    }

    public static StorageConfig sqlite(String file) {
        return new StorageConfig(StorageType.SQLITE, file, null, null);
    }
}
