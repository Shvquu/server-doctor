package com.serverdoctor.core.advisory;

import com.serverdoctor.common.model.PluginInfo;

import java.util.List;

/** Default source: no advisory feed configured -> no advisory findings. */
public final class NoopAdvisorySource implements AdvisorySource {

    public static final NoopAdvisorySource INSTANCE = new NoopAdvisorySource();

    private NoopAdvisorySource() {}

    @Override public List<Advisory> findAdvisories(PluginInfo plugin) { return List.of(); }
}
