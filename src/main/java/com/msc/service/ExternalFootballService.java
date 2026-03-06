package com.msc.service;

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


}
