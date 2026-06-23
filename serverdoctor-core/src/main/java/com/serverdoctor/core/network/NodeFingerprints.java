package com.serverdoctor.core.network;

import com.serverdoctor.common.model.NodeFingerprint;
import com.serverdoctor.common.model.PluginInfo;
import com.serverdoctor.platform.ServerPlatform;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NodeFingerprints {

    private static final Pattern MC = Pattern.compile("MC:\\s*([0-9][0-9.]*)");

    private NodeFingerprints() {}

    public static NodeFingerprint of(ServerPlatform platform, String nodeId) {
        Map<String, String> plugins = new LinkedHashMap<>();
        for (PluginInfo p : platform.plugins().installed()) {
            if (p.version() != null && !p.version().isBlank()) plugins.put(p.name(), p.version());
        }
        return new NodeFingerprint(
                nodeId,
                platform.name(),
                minecraftVersion(platform.serverInfo().version()),
                platform.serverInfo().javaVersion(),
                plugins,
                Instant.now());
    }

    /** "git-Paper (MC: 1.21.4)" -> "1.21.4"; otherwise the trimmed input. */
    public static String minecraftVersion(String rawVersion) {
        if (rawVersion == null) return "";
        Matcher m = MC.matcher(rawVersion);
        return m.find() ? m.group(1) : rawVersion.trim();
    }

}
