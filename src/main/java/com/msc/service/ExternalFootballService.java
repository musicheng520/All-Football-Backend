package com.msc.service;

public interface ExternalFootballService {
    String fetchTeams(Long leagueId, Integer season);

    void syncTeams(Integer season);

    void syncFixtures(Integer season, Long leagueId);

    void syncAllFixtures(Integer season);

    void syncPlayers(Integer season);


}
