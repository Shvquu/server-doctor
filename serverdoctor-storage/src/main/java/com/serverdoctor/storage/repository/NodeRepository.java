package com.serverdoctor.storage.repository;

import com.serverdoctor.common.model.NodeFingerprint;

import java.util.List;

/**
 * Stores one row/document per ServerDoctor node (keyed by nodeId) in the shared backend, so nodes
 * can read each other's fingerprints for cross-node consistency checks.
 */
public interface NodeRepository {

    /** Inserts or updates a node fingerprint. */
    void upsert(NodeFingerprint fingerprint);

    /** Returns all node fingerprints. */
    List<NodeFingerprint> findAll();

    /** Returns all node fingerprints except the one with the given ID. */
    List<NodeFingerprint> others(String localNodeID);
}
