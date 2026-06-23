package com.serverdoctor.core.network;

import com.serverdoctor.common.model.NodeFingerprint;

import java.util.List;

/** Default: no network view. */
public final class NoopNetworkView implements NetworkView {
    public static final NoopNetworkView INSTANCE = new NoopNetworkView();
    private NoopNetworkView() {}
    @Override public List<NodeFingerprint> remoteNodes() { return List.of(); }
}
