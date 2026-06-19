package com.serverdoctor.core.compat;

import com.serverdoctor.common.model.PluginInfo;

import java.util.Optional;

/** Default: no metadata feed -> only runtime signals are used. */
public final class NoopCompatibilityMetadataSource implements CompatibilityMetadataSource {

    public static final NoopCompatibilityMetadataSource INSTANCE = new NoopCompatibilityMetadataSource();

    private NoopCompatibilityMetadataSource() {}

    @Override public Optional<CompatMetadata> lookup(PluginInfo plugin) { return Optional.empty(); }
}
