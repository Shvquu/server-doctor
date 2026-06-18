package com.serverdoctor.core.advisory;

import com.serverdoctor.common.model.PluginInfo;

import java.util.List;

/**
 * Supplies known security advisories for installed plugins. Implementations must be
 * read-only and offline-tolerant: if no data is available, return an empty list - never
 * guess and never invent advisories.
 */
public interface AdvisorySource {

    List<Advisory> findAdvisories(PluginInfo plugin);
}
