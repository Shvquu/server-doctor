package com.serverdoctor.core.network;

import com.serverdoctor.common.model.NodeFingerprint;

import java.util.List;

/**
 * Read-only view of the <em>other</em> ServerDoctor nodes in the network (everyone except this
 * node), sourced from the shared storage backend. Default is empty, so cross-node checks stay
 * silent on a single node or when no shared database is configured.
 */
@FunctionalInterface
public interface NetworkView {
    List<NodeFingerprint> remoteNodes();
}
