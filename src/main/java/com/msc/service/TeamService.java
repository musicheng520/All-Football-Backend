package com.msc.service;

import com.msc.model.entity.Team;
import com.msc.result.PageResult;

public interface TeamService {
    Team findById(Long id);

    void delete(Long id);

    void update(Team team);

    void create(Team team);

    PageResult<Team> page(int page, int size);
}
