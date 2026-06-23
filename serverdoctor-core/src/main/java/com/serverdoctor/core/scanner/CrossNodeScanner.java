package com.serverdoctor.core.scanner;

import com.serverdoctor.api.module.AnalysisModule;
import com.serverdoctor.api.module.AnalysisResult;
import com.serverdoctor.api.module.ServerContext;
import com.serverdoctor.common.model.Capability;
import com.serverdoctor.common.model.Finding;
import com.serverdoctor.common.model.NodeFingerprint;
import com.serverdoctor.common.model.PluginInfo;
import com.serverdoctor.common.model.Severity;
import com.serverdoctor.core.network.NetworkView;
import com.serverdoctor.core.network.NoopNetworkView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Compares this node against the other nodes in the network (via the shared storage) and reports
 * inconsistencies: differing server (Minecraft) versions, differing Java major versions, and the
 * same plugin running at different versions across nodes.
 *
 * <p>Uses the injected {@link NetworkView}; stays silent on a single node.
 */
public final class CrossNodeScanner implements AnalysisModule {

    private static final Pattern MC = Pattern.compile("MC:\\s*([0-9][0-9.]*)");
    private static final int MAX_LISTED = 6;

    private final NetworkView view;

    public CrossNodeScanner() { this(NoopNetworkView.INSTANCE); }

    public CrossNodeScanner(NetworkView view) {
        this.view = view == null ? NoopNetworkView.INSTANCE : view;
    }

    @Override public String id() { return "cross-node"; }

    @Override public Set<Capability> requiredCapabilities() { return Set.of(Capability.HAS_PLUGINS); }

    @Override
    public AnalysisResult analyze(ServerContext context) {
        AnalysisResult.Builder out = AnalysisResult.builder(id());

        List<NodeFingerprint> remotes = view.remoteNodes();
        if (remotes == null || remotes.isEmpty()) return out.build();

        List<NodeFingerprint> all = new ArrayList<>();
        all.add(local(context));
        all.addAll(remotes);

        // server (MC) version divergence
        Set<String> servers = new TreeSet<>();
        for (NodeFingerprint n : all) if (notBlank(n.serverVersion())) servers.add(n.serverVersion());
        if (servers.size() > 1) {
            out.finding(new Finding(id(), Severity.MEDIUM,
                    "Server versions differ across " + all.size() + " nodes: " + String.join(", ", servers)));
        }

        // java major divergence
        Set<String> javas = new TreeSet<>();
        for (NodeFingerprint n : all) { String m = major(n.javaVersion()); if (notBlank(m)) javas.add(m); }
        if (javas.size() > 1) {
            out.finding(new Finding(id(), Severity.LOW,
                    "Java major versions differ across nodes: " + String.join(", ", javas)));
        }

        // per-plugin version divergence
        Map<String, Set<String>> versionsByPlugin = new TreeMap<>();
        for (NodeFingerprint n : all) {
            for (Map.Entry<String, String> e : n.pluginVersions().entrySet()) {
                if (notBlank(e.getValue())) {
                    versionsByPlugin.computeIfAbsent(e.getKey(), k -> new LinkedHashSet<>()).add(e.getValue());
                }
            }
        }
        List<String> diverging = new ArrayList<>();
        for (Map.Entry<String, Set<String>> e : versionsByPlugin.entrySet()) {
            if (e.getValue().size() > 1) diverging.add(e.getKey() + " (" + String.join(" / ", e.getValue()) + ")");
        }
        if (!diverging.isEmpty()) {
            String list = diverging.size() > MAX_LISTED
                    ? String.join("; ", diverging.subList(0, MAX_LISTED)) + "; +" + (diverging.size() - MAX_LISTED) + " more"
                    : String.join("; ", diverging);
            out.finding(new Finding(id(), Severity.MEDIUM,
                    "Plugins running at different versions across nodes: " + list));
        }

        return out.build();
    }

    private static NodeFingerprint local(ServerContext context) {
        String platform = context.serverInfo().platform();
        String server = extractMc(context.serverInfo().version());
        String java = context.serverInfo().javaVersion();
        Map<String, String> plugins = new LinkedHashMap<>();
        for (PluginInfo p : context.plugins()) {
            if (notBlank(p.version())) plugins.put(p.name(), p.version());
        }
        return new NodeFingerprint("local", platform, server, java, plugins, null);
    }

    static String extractMc(String version) {
        if (version == null) return "";
        Matcher m = MC.matcher(version);
        return m.find() ? m.group(1) : version.trim();
    }

    static String major(String javaVersion) {
        if (javaVersion == null || javaVersion.isBlank()) return "";
        String v = javaVersion.trim();
        if (v.startsWith("1.")) v = v.substring(2);            // 1.8.x -> 8
        int dot = v.indexOf('.');
        String head = dot > 0 ? v.substring(0, dot) : v;
        int us = head.indexOf('_'); if (us > 0) head = head.substring(0, us);
        return head;
    }

    private static boolean notBlank(String s) { return s != null && !s.isBlank(); }
}
