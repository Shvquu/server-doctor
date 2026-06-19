package com.serverdoctor.core.engine;

import com.serverdoctor.core.advisory.AdvisorySource;
import com.serverdoctor.core.advisory.NoopAdvisorySource;
import com.serverdoctor.core.compat.CompatibilityMetadataSource;
import com.serverdoctor.core.compat.NoopCompatibilityMetadataSource;
import com.serverdoctor.core.regression.NoopPerformanceHistory;
import com.serverdoctor.core.regression.PerformanceHistory;

public final class ScannerSources {

    private final AdvisorySource advisory;
    private final CompatibilityMetadataSource compatibility;
    private final PerformanceHistory history;

    private ScannerSources(Builder b) {
        this.advisory = b.advisorySource;
        this.compatibility = b.compatibilityMetadataSource;
        this.history = b.performanceHistory;
    }

    public AdvisorySource advisory() { return advisory; }
    public CompatibilityMetadataSource compatibility() { return compatibility; }
    public PerformanceHistory history() { return history; }

    public static ScannerSources none() { return builder().build(); }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private AdvisorySource advisorySource = NoopAdvisorySource.INSTANCE;
        private CompatibilityMetadataSource compatibilityMetadataSource = NoopCompatibilityMetadataSource.INSTANCE;
        private PerformanceHistory performanceHistory = NoopPerformanceHistory.INSTANCE;

        public Builder advisory(AdvisorySource v) {
            this.advisorySource = v == null ? NoopAdvisorySource.INSTANCE : v; return this;
        }
        public Builder compatibility(CompatibilityMetadataSource v) {
            this.compatibilityMetadataSource = v == null ? NoopCompatibilityMetadataSource.INSTANCE : v; return this;
        }
        public Builder history(PerformanceHistory v) {
            this.performanceHistory = v == null ? NoopPerformanceHistory.INSTANCE : v; return this;
        }
        public ScannerSources build() { return new ScannerSources(this); }
    }

}
