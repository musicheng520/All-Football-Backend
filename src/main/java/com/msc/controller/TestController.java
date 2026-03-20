package com.msc.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestController {

    private final SimpMessagingTemplate simpMessagingTemplate;

    @GetMapping("/push/goal/{matchId}")
    public String pushGoal(@PathVariable Long matchId) {

        Map<String, Object> data = new HashMap<>();

        // 随机比分（模拟变化）
        int home = (int) (Math.random() * 4);
        int away = (int) (Math.random() * 4);

        data.put("fixture", Map.of(
                "status", Map.of("short", "1H")
        ));

        data.put("goals", Map.of(
                "home", home,
                "away", away
        ));

        simpMessagingTemplate.convertAndSend("/topic/match/" + matchId, data);

        return "Goal pushed!";
    }
}