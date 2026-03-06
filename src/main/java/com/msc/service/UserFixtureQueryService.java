package com.msc.service;

import com.msc.model.entity.Fixture;
import com.msc.result.PageResult;

public interface UserFixtureQueryService {
    PageResult<Fixture> page(int page,
                             int size,
                             Long leagueId,
                             Integer season);
}
