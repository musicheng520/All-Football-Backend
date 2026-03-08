package com.msc.service;

import com.msc.model.entity.Team;
import com.msc.model.vo.TeamDetailVO;
import com.msc.model.vo.fixture.FixtureDetailVO;
import com.msc.result.PageResult;

public interface ExternalFootballService {
    String fetchTeams(Long leagueId, Integer season);

    PageResult<Team> fetchTeamsForQuery(Long leagueId, Integer season, int page, int size);
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

    TeamDetailVO fetchTeamDetail(Long teamId, Integer season);
}
