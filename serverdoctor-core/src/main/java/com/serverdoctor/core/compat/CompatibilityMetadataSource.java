package com.serverdoctor.core.compat;

import com.serverdoctor.common.model.PluginInfo;

import java.util.Optional;

/**
 * Supplies optional maintenance metadata (release age, Folia flag, known incompatibilities) for
 * a plugin. Read-only and offline-tolerant: return empty when nothing is known - never guess.
 */
public interface CompatibilityMetadataSource {

    Optional<CompatMetadata> lookup(PluginInfo plugin);
}
