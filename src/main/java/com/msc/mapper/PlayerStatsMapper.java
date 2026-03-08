package com.msc.mapper;

import com.msc.model.entity.PlayerStats;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PlayerStatsMapper {

    void upsert(PlayerStats stats);

    List<PlayerStats> findByPlayerAndSeason(
            @Param("playerId") Long playerId,
            @Param("season") Integer season
    );
}