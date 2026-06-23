package com.serverdoctor.common.model;

import java.time.Instant;
import java.util.Map;

/**
 * Identity + key facts of one ServerDoctor node in a network, persisted to the shared storage so
 * other nodes can compare against it. {@code pluginVersions} maps plugin name -> version.
 */
public record NodeFingerprint(String nodeId, String platform, String serverVersion, String javaVersion, Map<String, String> pluginVersions, Instant capturedAt) {
    public NodeFingerprint {
        pluginVersions = pluginVersions == null ? Map.of() : Map.copyOf(pluginVersions);
    }
}
