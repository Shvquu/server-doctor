package com.serverdoctor.core.config;

/** Supplies parsed server configuration. Read-only and offline-tolerant (missing files = empty). */
public interface ConfigSource {
    ConfigSnapshot read();
}
