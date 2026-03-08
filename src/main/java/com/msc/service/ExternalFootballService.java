package com.msc.service;

import com.msc.model.vo.fixture.FixtureDetailVO;

public interface ExternalFootballService {
    String fetchTeams(Long leagueId, Integer season);

    void syncTeams(Integer season);

    void syncFixtures(Integer season, Long leagueId);

    void syncAllFixtures(Integer season);

    void syncPlayers(Integer season);

    void syncPlayerStats(Integer season);

    void syncMatchEvents(Integer season);

    void syncLineups(Integer season);

    void syncYesterdayMatches();

    void weeklyBaseSync(Integer season);

    void syncMatchStatistics(Integer season);

    void refreshLiveSnapshotToRedis();

    String fetchLiveFixturesFilteredJson();

    String fetchFixtureById(Long fixtureId);


    String fetchFixturesBySeason(Long leagueId, Integer season);

    FixtureDetailVO buildFixtureDetailFromDb(Long fixtureId);

    FixtureDetailVO fetchHistoricalFixtureDetail(Long fixtureId);
}
