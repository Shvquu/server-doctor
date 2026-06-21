package com.serverdoctor.core.config;

/** Default: no config inspection. */
public final class NoopConfigSource implements ConfigSource {

    public static final NoopConfigSource INSTANCE = new NoopConfigSource();

    private NoopConfigSource() {}

    @Override public ConfigSnapshot read() { return ConfigSnapshot.empty(); }
}
