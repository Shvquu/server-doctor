package com.serverdoctor.core.scanner;

import com.serverdoctor.api.module.AnalysisModule;
import com.serverdoctor.api.module.AnalysisResult;
import com.serverdoctor.api.module.ServerContext;
import com.serverdoctor.common.model.Capability;
import com.serverdoctor.common.model.Finding;
import com.serverdoctor.common.model.PluginInfo;
import com.serverdoctor.common.model.Severity;
import com.serverdoctor.core.compat.CompatMetadata;
import com.serverdoctor.core.compat.CompatibilityMetadataSource;
import com.serverdoctor.core.compat.NoopCompatibilityMetadataSource;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Assesses each plugin's compatibility with the running server and aggregates the signals into a
 * transparent 0-100 risk score.
 *
 * <p>Runtime signals (no network): declared {@code api-version} vs. the server's Minecraft
 * version (Paper/Folia only), whether Folia support can be confirmed, and enabled state. An
 * optional {@link CompatibilityMetadataSource} adds release age, a Folia flag and known
 * incompatibilities from a real external feed. Works on Paper/Folia, Velocity and BungeeCord;
 * on proxies the Minecraft-version and Folia signals are simply skipped (degrades gracefully).
 */
public final class CompatibilityScanner implements AnalysisModule {

    private static final Pattern MC = Pattern.compile("(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?");

    private final CompatibilityMetadataSource metadata;

    public CompatibilityScanner() {
        this(NoopCompatibilityMetadataSource.INSTANCE);
    }

    public CompatibilityScanner(CompatibilityMetadataSource metadata) {
        this.metadata = metadata == null ? NoopCompatibilityMetadataSource.INSTANCE : metadata;
    }

    @Override public String id() { return "compatibility"; }

    @Override public Set<Capability> requiredCapabilities() { return Set.of(Capability.HAS_PLUGINS); }

    @Override
    public AnalysisResult analyze(ServerContext context) {
        AnalysisResult.Builder out = AnalysisResult.builder(id());

        boolean proxy = context.has(Capability.IS_PROXY);
        boolean folia = context.has(Capability.HAS_REGIONS);
        int[] serverMc = proxy ? null : parse(extractMc(context.serverInfo().version()));

        for (PluginInfo p : context.plugins()) {
            int score = 0;
            List<String> reasons = new ArrayList<>();

            // 1) api-version vs server Minecraft version (Paper/Folia only)
            if (!proxy && serverMc != null) {
                if (!p.hasApiVersion()) {
                    score += 20;
                    reasons.add("no api-version declared (legacy plugin)");
                } else {
                    int[] api = parse(p.apiVersion());
                    if (api != null && api[0] == serverMc[0] && api[1] < serverMc[1]) {
                        int gap = serverMc[1] - api[1];
                        score += Math.min(gap * 8, 40);
                        reasons.add("built for API " + p.apiVersion() + " (server " + render(serverMc) + ")");
                    }
                }
            }

            Optional<CompatMetadata> md = metadata.lookup(p);
            Boolean foliaSupported = md.map(CompatMetadata::foliaSupported).orElse(null);

            // 2) Folia compatibility (only relevant when we actually run on Folia)
            if (folia) {
                if (Boolean.FALSE.equals(foliaSupported)) {
                    score += 40;
                    reasons.add("not Folia-compatible (per feed)");
                } else if (foliaSupported == null) {
                    score += 15;
                    reasons.add("Folia support unverified");
                }
            }

            // 3) disabled plugin
            if (!p.enabled()) {
                score += 10;
                reasons.add("plugin is disabled");
            }

            // 4) release age (feed only)
            if (md.isPresent() && md.get().lastUpdated() != null) {
                long days = ChronoUnit.DAYS.between(md.get().lastUpdated(), LocalDate.now());
                if (days > 730) { score += 35; reasons.add("last update " + days + " days ago"); }
                else if (days > 365) { score += 20; reasons.add("last update " + days + " days ago"); }
            }

            // 5) known incompatibility / maintenance note (feed only)
            if (md.isPresent() && md.get().note() != null && !md.get().note().isBlank()) {
                score += 30;
                reasons.add(md.get().note().trim());
            }

            score = Math.min(score, 100);
            Severity severity = severityOf(score);
            if (!severity.atLeast(Severity.LOW)) continue; // only report concerning plugins

            String ref = md.map(CompatMetadata::reference)
                    .filter(r -> r != null && !r.isBlank())
                    .map(r -> " (" + r + ")").orElse("");
            out.finding(new Finding(id(), severity,
                    p.name() + " - Risk " + score + "/100: " + String.join("; ", reasons) + ref));
        }

        return out.build();
    }

    private static Severity severityOf(int score) {
        if (score >= 90) return Severity.CRITICAL;
        if (score >= 70) return Severity.HIGH;
        if (score >= 40) return Severity.MEDIUM;
        if (score >= 15) return Severity.LOW;
        return Severity.OK;
    }

    /** Pulls "1.21.4" out of strings like "git-Paper-123 (MC: 1.21.4)" or a bare version. */
    private static String extractMc(String version) {
        if (version == null) return null;
        int i = version.indexOf("MC:");
        if (i >= 0) {
            Matcher m = MC.matcher(version.substring(i));
            if (m.find()) return m.group();
        }
        return version;
    }

    private static int[] parse(String version) {
        if (version == null || version.isBlank()) return null;
        Matcher m = MC.matcher(version.trim());
        if (!m.find()) return null;
        int major = Integer.parseInt(m.group(1));
        int minor = m.group(2) == null ? 0 : Integer.parseInt(m.group(2));
        int patch = m.group(3) == null ? 0 : Integer.parseInt(m.group(3));
        return new int[]{major, minor, patch};
    }

    private static String render(int[] v) {
        return v[2] > 0 ? v[0] + "." + v[1] + "." + v[2] : v[0] + "." + v[1];
    }
}
