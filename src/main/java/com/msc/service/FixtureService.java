package com.msc.service;

import com.msc.model.entity.Fixture;
import com.msc.result.PageResult;

public interface FixtureService {

    Fixture findById(Long id);

    PageResult<Fixture> page(int page, int size, Long leagueId, Integer season);

    void create(Fixture fixture);

    void update(Fixture fixture);

    void delete(Long id);

    void updateScore(Long id, Integer homeScore, Integer awayScore,
                     Integer elapsed, String status);
}