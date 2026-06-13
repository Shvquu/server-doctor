package com.serverdoctor.platform;

import com.serverdoctor.common.model.PluginInfo;

import java.util.List;
import java.util.Optional;

/** Read-only-Zugriff auf installierte Plugins. */
public interface PluginAdapter {

    List<PluginInfo> installed();

    default Optional<PluginInfo> byName(String name) {
        return installed().stream()
                .filter(p -> p.name().equalsIgnoreCase(name))
                .findFirst();
    }
}
