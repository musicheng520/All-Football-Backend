package com.msc.service.impl;

import com.msc.mapper.FixtureMapper;
import com.msc.mapper.TeamMapper;
import com.msc.model.entity.Fixture;
import com.msc.model.entity.Team;
import com.msc.result.PageResult;
import com.msc.service.FixtureService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FixtureServiceImpl implements FixtureService {

    private final FixtureMapper fixtureMapper;
    private final TeamMapper teamMapper;

    @Override
    public Fixture findById(Long id) {
        return fixtureMapper.findById(id);
    }

    @Override
    public PageResult<Fixture> page(int page, int size,
                                    Long leagueId, Integer season) {

        int offset = (page - 1) * size;

        long total = fixtureMapper.count(leagueId, season);
        List<Fixture> list =
                fixtureMapper.findPage(offset, size, leagueId, season);

        return new PageResult<>(
                total,
                page,
                size,
                list
        );
    }

    @Override
    public void create(Fixture fixture) {

        //1. check if team exist
        Team home = teamMapper.findById(fixture.getHomeTeamId());
        Team away = teamMapper.findById(fixture.getAwayTeamId());

        if (home == null || away == null) {
            throw new RuntimeException("Team not found");
        }

        fixtureMapper.insert(fixture);
    }

    @Override
    public void update(Fixture fixture) {

        Fixture dbFixture = fixtureMapper.findById(fixture.getId());

        if (dbFixture == null) {
            throw new RuntimeException("Fixture not found");
        }

        // 2.can't modify finished game
        if ("FT".equals(dbFixture.getStatus())) {
            throw new RuntimeException("Match already finished");
        }

        fixtureMapper.update(fixture);
    }

    @Override
    public void delete(Long id) {

        Fixture dbFixture = fixtureMapper.findById(id);

        if (dbFixture == null) {
            throw new RuntimeException("Fixture not found");
        }

        if ("LIVE".equals(dbFixture.getStatus())) {
            throw new RuntimeException("Cannot delete live match");
        }

        fixtureMapper.delete(id);
    }

    @Override
    public void updateScore(Long id,
                            Integer homeScore,
                            Integer awayScore,
                            Integer elapsed,
                            String status) {

        Fixture dbFixture = fixtureMapper.findById(id);

        if (dbFixture == null) {
            throw new RuntimeException("Fixture not found");
        }

        if ("FT".equals(dbFixture.getStatus())) {
            throw new RuntimeException("Match already finished");
        }

        fixtureMapper.updateScore(id, homeScore, awayScore, elapsed, status);
    }
}