package com.msc.service.query;

import com.msc.model.entity.Team;
import com.msc.model.vo.TeamDetailVO;
import com.msc.result.PageResult;

public interface TeamQueryService {

    TeamDetailVO getTeamDetail(Long teamId, Integer season);

    PageResult<Team> getTeamList(int page, int size, Long leagueId, Integer season);

}