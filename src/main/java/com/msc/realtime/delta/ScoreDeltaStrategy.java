package com.msc.realtime.delta;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

/**
 * Detect score changes.
 */
@Component
public class ScoreDeltaStrategy implements DeltaStrategy {


    @Override
    public boolean hasChanged(JsonNode oldNode, JsonNode newNode) {


        if (oldNode == null) return true;

        Integer oldHome = getInt(oldNode, "goals", "home");
        Integer oldAway = getInt(oldNode, "goals", "away");

        Integer newHome = getInt(newNode, "goals", "home");
        Integer newAway = getInt(newNode, "goals", "away");

        return !safeEquals(oldHome, newHome)
                || !safeEquals(oldAway, newAway);


    }

    @Override
    public String getType() {
        return "SCORE_CHANGE";
    }

    private Integer getInt(JsonNode node, String parent, String field) {

        JsonNode p = node.get(parent);
        if (p == null || p.get(field) == null || p.get(field).isNull()) {
            return null;
        }

        return p.get(field).asInt();
    }

    private boolean safeEquals(Object a, Object b) {
        if (a == null) return b == null;
        return a.equals(b);
    }
}