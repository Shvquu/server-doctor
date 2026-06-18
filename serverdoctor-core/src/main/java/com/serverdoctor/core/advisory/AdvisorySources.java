package com.serverdoctor.core.advisory;

import java.time.Duration;
import java.util.function.Consumer;

/** Convenience factory so platform adapters can build a source from config in one line. */
public final class AdvisorySources {

    private AdvisorySources() {}

    public static AdvisorySource disabled() {
        return NoopAdvisorySource.INSTANCE;
    }

    public static AdvisorySource remote(String feedUrl, long refreshMinutes, Consumer<String> log) {
        if (feedUrl == null || feedUrl.isBlank()) return disabled();
        return new RemoteAdvisorySource(feedUrl, Duration.ofMinutes(Math.max(1L, refreshMinutes)), log);
    }
}
