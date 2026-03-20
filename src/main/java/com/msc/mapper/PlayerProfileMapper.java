package com.msc.mapper;

import com.msc.model.entity.PlayerProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PlayerProfileMapper {

    // 查询（缓存用）
    PlayerProfile findByPlayerId(@Param("playerId") Long playerId);

    // 新增或更新（核心）
    void insertOrUpdate(PlayerProfile profile);

    // 可选：删除（不一定需要）
    void deleteByPlayerId(@Param("playerId") Long playerId);
}