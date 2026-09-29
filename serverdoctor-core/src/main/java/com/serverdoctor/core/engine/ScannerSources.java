package com.serverdoctor.core.engine;

import com.serverdoctor.core.advisory.AdvisorySource;
import com.serverdoctor.core.advisory.NoopAdvisorySource;
import com.serverdoctor.core.compat.CompatibilityMetadataSource;
import com.serverdoctor.core.compat.NoopCompatibilityMetadataSource;
import com.serverdoctor.core.config.ConfigSource;
import com.serverdoctor.core.config.NoopConfigSource;
import com.serverdoctor.core.network.NetworkView;
import com.serverdoctor.core.network.NoopNetworkView;
import com.serverdoctor.core.regression.NoopPerformanceHistory;
import com.serverdoctor.core.regression.NoopScanHistory;
import com.serverdoctor.core.regression.PerformanceHistory;
import com.serverdoctor.core.regression.ScanHistory;

/**
 * Bundles the optional, externally-provided sources a platform adapter can inject into the core:
 * the security advisory feed, the compatibility metadata feed, the performance and scan-count
 * histories (for regression detection) and the config-file source. All default to no-op, so leaving any unset
 * is safe.
 */
public final class ScannerSources {

    private final AdvisorySource advisory;
    private final CompatibilityMetadataSource compatibility;
    private final PerformanceHistory history;
    private final ScanHistory scanHistory;
    private final ConfigSource config;
    private final NetworkView network;

    private ScannerSources(Builder b) {
        this.advisory = b.advisory;
        this.compatibility = b.compatibility;
        this.history = b.history;
        this.scanHistory = b.scanHistory;
        this.config = b.config;
        this.network = b.network;
    }

    public AdvisorySource advisory() { return advisory; }
    public CompatibilityMetadataSource compatibility() { return compatibility; }
    public PerformanceHistory history() { return history; }
    public ScanHistory scanHistory() { return scanHistory; }
    public ConfigSource config() { return config; }
    public NetworkView network() { return network; }

    public static ScannerSources none() { return builder().build(); }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private AdvisorySource advisory = NoopAdvisorySource.INSTANCE;
        private CompatibilityMetadataSource compatibility = NoopCompatibilityMetadataSource.INSTANCE;
        private PerformanceHistory history = NoopPerformanceHistory.INSTANCE;
        private ScanHistory scanHistory = NoopScanHistory.INSTANCE;
        private ConfigSource config = NoopConfigSource.INSTANCE;
        private NetworkView network = NoopNetworkView.INSTANCE;

        public Builder advisory(AdvisorySource v) {
            this.advisory = v == null ? NoopAdvisorySource.INSTANCE : v; return this;
        }
        public Builder compatibility(CompatibilityMetadataSource v) {
            this.compatibility = v == null ? NoopCompatibilityMetadataSource.INSTANCE : v; return this;
        }
        public Builder history(PerformanceHistory v) {
            this.history = v == null ? NoopPerformanceHistory.INSTANCE : v; return this;
        }
        public Builder scanHistory(ScanHistory v) {
            this.scanHistory = v == null ? NoopScanHistory.INSTANCE : v; return this;
        }
        public Builder config(ConfigSource v) {
            this.config = v == null ? NoopConfigSource.INSTANCE : v; return this;
        }
        public Builder network(NetworkView v) {
            this.network = v == null ? NoopNetworkView.INSTANCE : v; return this;
        }
        public ScannerSources build() { return new ScannerSources(this); }
    }
}
