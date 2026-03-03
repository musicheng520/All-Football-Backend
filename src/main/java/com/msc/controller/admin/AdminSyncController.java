package com.msc.controller.admin;

import com.msc.config.FootballProperties;
import com.msc.result.Result;
import com.msc.service.ExternalFootballService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/sync")
@RequiredArgsConstructor
public class AdminSyncController {

    private final ExternalFootballService externalFootballService;
    private final FootballProperties footballProperties;

    /**
     * Test Teams API connectivity
     */
    @PostMapping("/test-teams")
    public Result testTeams(@RequestParam(required = false) Integer season) {

        // set season to default if get no season
        Integer useSeason = season != null
                ? season
                : footballProperties.getDefaultSeason();

        // test with first league
        Long testLeague = footballProperties
                .getSupportedLeagues()
                .get(0);

        String response =
                externalFootballService.fetchTeams(testLeague, useSeason);

        System.out.println("==== Teams API Response Length: "
                + response.length() + " ====");

        return Result.success(
                "Teams API OK, length = " + response.length()
        );
    }

    @PostMapping("/teams")
    public Result syncTeams(@RequestParam(required = false) Integer season) {

        Integer useSeason = season != null
                ? season
                : footballProperties.getDefaultSeason();

        externalFootballService.syncTeams(useSeason);

        return Result.success("Teams synced");
    }

    @PostMapping("/fixtures/test")
    public Result syncFixturesTest(@RequestParam Integer season) {

        externalFootballService.syncFixtures(season, 39L);

        return Result.success("Fixtures synced for league 39");
    }

    @PostMapping("/players")
    public Result syncPlayers(@RequestParam Integer season) {
        externalFootballService.syncPlayers(season);
        return Result.success("Players synced.");
    }


    @PostMapping("/player-stats")
    public Result syncPlayerStats(@RequestParam Integer season) {
        externalFootballService.syncPlayerStats(season);
        return Result.success("Player stats synced.");
    }

    @PostMapping("/fixtures")
    public Result syncAllFixtures(@RequestParam Integer season) {

        externalFootballService.syncAllFixtures(season);

        return Result.success("All fixtures synced.");
    }

    /**
     * Test Live API once (no redis)
     */
    @PostMapping("/live/test")
    public Result testLiveOnce() {

        String json = externalFootballService.fetchLiveFixturesFilteredJson();

        return Result.success("Live fetched, length = " + json.length());
    }


    /**
     * Refresh Live Snapshot into Redis manually
     */
    @PostMapping("/live/refresh")
    public Result refreshLiveSnapshot() {

        externalFootballService.refreshLiveSnapshotToRedis();

        return Result.success("Live snapshot saved to Redis.");
    }
}