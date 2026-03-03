package com.msc.scheduler;

import com.msc.service.ExternalFootballService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LiveSnapshotScheduler {

    private final ExternalFootballService externalFootballService;

    // 每60秒执行一次
    @Scheduled(fixedRate = 60000)
    public void refreshLiveSnapshot() {

        System.out.println("=== Live Scheduler Running ===");

        externalFootballService.refreshLiveSnapshotToRedis();
    }
}