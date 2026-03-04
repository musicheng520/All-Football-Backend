package com.msc.realtime.delta;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Central manager to execute all delta strategies.
 */
@Component
@RequiredArgsConstructor
public class DeltaManager {

    private final List<DeltaStrategy> strategies;

    /**
     * Detect all change types between old and new match snapshot.
     */
    public List<String> detectChanges(JsonNode oldNode, JsonNode newNode) {

        List<String> changes = new ArrayList<>();

        for (DeltaStrategy strategy : strategies) {

            if (strategy.hasChanged(oldNode, newNode)) {
                changes.add(strategy.getType());
            }
        }

        return changes;
    }
}