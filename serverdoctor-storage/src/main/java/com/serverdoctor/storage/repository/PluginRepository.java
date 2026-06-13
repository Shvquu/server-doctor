package com.serverdoctor.storage.repository;

import com.serverdoctor.common.model.PluginInfo;

import java.time.Instant;
import java.util.List;

public interface PluginRepository {
    void saveInventory(Instant at, List<PluginInfo> plugins);
    /** Plugins des jüngsten gespeicherten Inventars. */
    List<PluginInfo> latestInventory();
}
