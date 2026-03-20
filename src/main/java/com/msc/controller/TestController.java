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

    @GetMapping("/mock/goal/{matchId}")
    public void mockGoal(@PathVariable Long matchId) {

        Map<String, Object> data = Map.of(
                "fixture", Map.of(
                        "id", matchId,
                        "status", Map.of("short", "LIVE")
                ),
                "goals", Map.of(
                        "home", (int)(Math.random()*3),
                        "away", (int)(Math.random()*3)
                ),
                "teams", Map.of(
                        "home", Map.of("name", "Barcelona"),
                        "away", Map.of("name", "Real Madrid")
                )
        );

        simpMessagingTemplate.convertAndSend("/topic/match", data);
    }

    @GetMapping("/mock/ft/{matchId}")
    public void mockFT(@PathVariable Long matchId) {

        Map<String, Object> data = Map.of(
                "fixture", Map.of(
                        "id", matchId,
                        "status", Map.of("short", "FT")
                )
        );

        simpMessagingTemplate.convertAndSend("/topic/match", data);
    }
}