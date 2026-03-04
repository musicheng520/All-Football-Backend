package com.msc.realtime.delta;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Strategy interface for delta detection.
 * Each implementation checks a specific type of change.
 */
public interface DeltaStrategy {

    /**
     * Detect whether change occurred between old and new snapshot.
     */
    boolean hasChanged(JsonNode oldNode, JsonNode newNode);

    /**
     * Type of delta (e.g. SCORE_CHANGE, STATUS_CHANGE)
     */
    String getType();
}