package com.msc.realtime.delta;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

@Component
public class EventDeltaStrategy implements DeltaStrategy {

    @Override
    public boolean hasChanged(JsonNode oldMatch, JsonNode newMatch) {

        if (oldMatch == null) {
            return false; // 冷启动不算
        }

        JsonNode oldEvents = oldMatch.get("events");
        JsonNode newEvents = newMatch.get("events");

        if (oldEvents == null || newEvents == null) {
            return false;
        }

        int oldSize = oldEvents.size();
        int newSize = newEvents.size();

        // 事件数量变化
        if (oldSize != newSize) {
            return true;
        }

        if (newSize == 0) {
            return false;
        }

        // 比较最后一个事件关键字段
        JsonNode oldLast = oldEvents.get(oldSize - 1);
        JsonNode newLast = newEvents.get(newSize - 1);

        int oldTime = oldLast.get("time").get("elapsed").asInt();
        int newTime = newLast.get("time").get("elapsed").asInt();

        String oldType = oldLast.get("type").asText();
        String newType = newLast.get("type").asText();

        long oldPlayer = oldLast.get("player").get("id").asLong();
        long newPlayer = newLast.get("player").get("id").asLong();

        return oldTime != newTime
                || !oldType.equals(newType)
                || oldPlayer != newPlayer;
    }

    @Override
    public String getType() {
        return "EVENT_CHANGE";
    }
}