package com.msc.service.query;

import com.msc.model.entity.Player;
import com.msc.model.vo.PlayerDetailVO;
import com.msc.result.PageResult;

public interface PlayerQueryService {

    PlayerDetailVO getPlayerDetail(Long playerId, Integer season);

    PageResult<Player> getPlayerList(
            int page,
            int size,
            Long teamId,
            Integer season
    );
}