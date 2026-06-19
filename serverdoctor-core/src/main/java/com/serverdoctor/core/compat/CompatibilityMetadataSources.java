package com.serverdoctor.core.compat;

import java.time.Duration;
import java.util.function.Consumer;

/** Factory so platform adapters can build a metadata source from config in one line. */
public final class CompatibilityMetadataSources {

    private CompatibilityMetadataSources() {}

    public static CompatibilityMetadataSource disabled() {
        return NoopCompatibilityMetadataSource.INSTANCE;
    }

    public static CompatibilityMetadataSource remote(String feedUrl, long refreshMinutes, Consumer<String> log) {
        if (feedUrl == null || feedUrl.isBlank()) return disabled();
        return new RemoteCompatibilityMetadataSource(feedUrl, Duration.ofMinutes(Math.max(1L, refreshMinutes)), log);
    }
}
