package com.serverdoctor.storage.node;

import com.serverdoctor.common.model.NodeFingerprint;
import com.serverdoctor.storage.repository.NodeRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Reference in-memory implementation (single-node / testing). */
public final class InMemoryNodeRepository implements NodeRepository {

    private final Map<String, NodeFingerprint> nodes = new LinkedHashMap<>();

    @Override
    public void upsert(NodeFingerprint fingerprint) {
        if (fingerprint != null && fingerprint.nodeId() != null) nodes.put(fingerprint.nodeId(), fingerprint);
    }

    @Override
    public List<NodeFingerprint> findAll() {
        return new ArrayList<>(nodes.values());
    }

    @Override
    public List<NodeFingerprint> others(String localNodeID) {
        List<NodeFingerprint> others = new ArrayList<>();
        for (NodeFingerprint fingerprint : nodes.values()) {
            if (localNodeID == null || !localNodeID.equals(fingerprint.nodeId())) others.add(fingerprint);
        }
        return others;
    }
}
