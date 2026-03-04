package com.msc.realtime.delta;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

/**
 * Detect match status changes (NS -> 1H -> HT -> 2H -> FT)
 */
@Component
public class StatusDeltaStrategy implements DeltaStrategy {

    @Override
    public boolean hasChanged(JsonNode oldNode, JsonNode newNode) {

        if (oldNode == null) return true;

        String oldStatus = getStatus(oldNode);
        String newStatus = getStatus(newNode);

        return !safeEquals(oldStatus, newStatus);
    }

    @Override
    public String getType() {
        return "STATUS_CHANGE";
    }

    private String getStatus(JsonNode node) {

        JsonNode fixture = node.get("fixture");
        if (fixture == null) return null;

        JsonNode status = fixture.get("status");
        if (status == null) return null;

        JsonNode shortCode = status.get("short");
        if (shortCode == null) return null;

        return shortCode.asText();
    }

    private boolean safeEquals(Object a, Object b) {
        if (a == null) return b == null;
        return a.equals(b);
    }
}