package com.msc.service.query;

import com.msc.model.entity.Fixture;
import com.msc.model.vo.fixture.FixtureDetailVO;
import com.msc.result.PageResult;

import java.util.List;

public interface UserFixtureQueryService {
    PageResult<Fixture> page(int page,
                             int size,
                             Long leagueId,
                             Integer season);

    FixtureDetailVO getFixtureDetail(Long fixtureId);

    List<Fixture> getLiveMatches();

    List<Fixture> getRecentMatches();

    List<Fixture> getUpcomingMatches();
}
