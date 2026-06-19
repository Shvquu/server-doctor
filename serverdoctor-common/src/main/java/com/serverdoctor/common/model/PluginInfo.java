package com.serverdoctor.common.model;

import java.util.List;

/** Read-only-Beschreibung eines installierten Plugins. */
public record PluginInfo(String name, String version, List<String> authors,
                         List<String> hardDepends, List<String> softDepends, boolean enabled,
                         String apiVersion) {

    public PluginInfo {
        authors      = authors      == null ? List.of() : List.copyOf(authors);
        hardDepends  = hardDepends  == null ? List.of() : List.copyOf(hardDepends);
        softDepends  = softDepends  == null ? List.of() : List.copyOf(softDepends);
        apiVersion   = apiVersion   == null ? "" : apiVersion;
    }

    /** Backward-compatible constructor for platforms that don't expose an api-version. */
    public PluginInfo(String name, String version, List<String> authors,
                      List<String> hardDepends, List<String> softDepends, boolean enabled) {
        this(name, version, authors, hardDepends, softDepends, enabled, "");
    }

    public boolean hasVersion() { return version != null && !version.isBlank(); }
    public boolean hasAuthors() { return !authors.isEmpty(); }
    public boolean hasApiVersion() { return apiVersion != null && !apiVersion.isBlank(); }
}
