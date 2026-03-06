package com.msc.scheduler;

import com.msc.config.FootballProperties;
import com.msc.service.ExternalFootballService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataSyncScheduler {

    private final ExternalFootballService externalFootballService;
    private final FootballProperties footballProperties;

    // every day 03:00
    @Scheduled(cron = "0 0 3 * * ?")
    public void yesterdaySync() {

        System.out.println("=== Yesterday Sync Running ===");

        externalFootballService.syncYesterdayMatches();
    }

    // every Sunday 04:00
    @Scheduled(cron = "0 0 4 ? * SUN")
    public void weeklyBaseSync() {

        Integer season = footballProperties.getDefaultSeason();

        System.out.println("=== Weekly Base Sync Running ===");

        externalFootballService.weeklyBaseSync(season);
    }
}