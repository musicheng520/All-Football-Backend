package com.msc.mapper;

import com.msc.model.entity.PlayerStats;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PlayerStatsMapper {

    void upsert(PlayerStats stats);
}