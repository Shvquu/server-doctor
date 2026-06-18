package com.serverdoctor.core.scanner;

import com.serverdoctor.api.module.AnalysisModule;
import com.serverdoctor.api.module.AnalysisResult;
import com.serverdoctor.api.module.ServerContext;
import com.serverdoctor.common.model.Capability;
import com.serverdoctor.common.model.PluginInfo;
import com.serverdoctor.common.model.SecurityRisk;
import com.serverdoctor.common.model.Severity;
import com.serverdoctor.core.advisory.Advisory;
import com.serverdoctor.core.advisory.AdvisorySource;
import com.serverdoctor.core.advisory.NoopAdvisorySource;

import java.util.Set;

/**
 * Conservative, honest maintenance/security heuristics, plus optional advisory lookups.
 *
 * <p>The metadata heuristic flags incomplete metadata. The advisory check consults a real,
 * external {@link AdvisorySource} (off by default) - no CVE database is invented here. When no
 * source is configured, the scanner behaves exactly as before.
 */
public final class SecurityScanner implements AnalysisModule {

    private final AdvisorySource advisories;

    /** Backward-compatible default: no advisory feed. */
    public SecurityScanner() {
        this(NoopAdvisorySource.INSTANCE);
    }

    public SecurityScanner(AdvisorySource advisories) {
        this.advisories = advisories == null ? NoopAdvisorySource.INSTANCE : advisories;
    }

    @Override public String id() { return "security"; }

    @Override public Set<Capability> requiredCapabilities() { return Set.of(Capability.HAS_PLUGINS); }

    @Override
    public AnalysisResult analyze(ServerContext context) {
        AnalysisResult.Builder out = AnalysisResult.builder(id());
        for (PluginInfo p : context.plugins()) {
            if (!p.hasVersion() || !p.hasAuthors()) {
                out.risk(new SecurityRisk(p.name(), SecurityRisk.RiskType.MISSING_METADATA,
                        Severity.LOW,
                        "Unvollständige Metadaten (Version/Autor) - Herkunft und Aktualität schwer prüfbar."));
            }
            for (Advisory advisory : advisories.findAdvisories(p)) {
                out.risk(new SecurityRisk(p.name(), SecurityRisk.RiskType.ADVISORY,
                        advisory.severity(), advisory.describe()));
            }
        }
        return out.build();
    }
}
