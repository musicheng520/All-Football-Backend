package com.msc.mapper;

import com.msc.model.entity.NewsPlayer;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface NewsPlayerMapper {

    void batchInsert(List<NewsPlayer> list);

    List<Long> findPlayerIdsByNewsId(Long newsId);

    void deleteByNewsId(Long newsId);

    List<Long> findNewsIdsByPlayerId(Long playerId);
}