package com.msc.service;

import com.msc.model.entity.Player;
import com.msc.result.PageResult;

public interface PlayerService {
    Player findById(Long id);

    void update(Player player);

    PageResult<Player> page(int page, int size, Long teamId, Integer season);

    void delete(Long id);

    void create(Player player);
}
