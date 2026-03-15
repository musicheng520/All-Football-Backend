package com.msc.mapper;

import com.msc.model.entity.Team;
import com.msc.result.PageResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TeamMapper {
    Team findById(Long id);


    List<Team> findBySeason(Integer season);

    void delete(Long id);

    void update(Team team);

    void insert(Team team);

    long count();

    List<Team> findByIds(@Param("ids") List<Long> ids);

    List<Team> findPage(@Param("offset") int offset,
                        @Param("size") int size);

    void upsert(Team team);

    List<Team> findByLeagueAndSeason(Long leagueId, Integer season);

    List<Team> searchByName(String name);
}
