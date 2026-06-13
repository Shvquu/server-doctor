package com.serverdoctor.common.model;

import java.util.List;

/** Read-only-Beschreibung eines installierten Plugins. */
public record PluginInfo(String name, String version, List<String> authors,
                         List<String> hardDepends, List<String> softDepends, boolean enabled) {

    public PluginInfo {
        authors      = authors      == null ? List.of() : List.copyOf(authors);
        hardDepends  = hardDepends  == null ? List.of() : List.copyOf(hardDepends);
        softDepends  = softDepends  == null ? List.of() : List.copyOf(softDepends);
    }

    public boolean hasVersion() { return version != null && !version.isBlank(); }
    public boolean hasAuthors() { return !authors.isEmpty(); }
}
