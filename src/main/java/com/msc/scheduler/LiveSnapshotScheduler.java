package com.msc.scheduler;

import com.msc.service.ExternalFootballService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class LiveSnapshotScheduler {

    private final ExternalFootballService externalFootballService;
    private final RedisTemplate<String, String> redisTemplate;

    // every 60s
    @Scheduled(fixedRate = 60000)
    public void refreshLiveSnapshot() {

        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent("live:scheduler:lock", "1", 55, TimeUnit.SECONDS);

        if (Boolean.FALSE.equals(locked)) {
            System.out.println("LiveScheduler skipped (lock exists)");
            return;
        }

        System.out.println("=== Live Scheduler Running ===");

        externalFootballService.refreshLiveSnapshotToRedis();
    }
}