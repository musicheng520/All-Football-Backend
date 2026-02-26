package com.msc.mapper;

import com.msc.model.entity.NewsTeam;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface NewsTeamMapper {

    void batchInsert(List<NewsTeam> list);

    List<Long> findTeamIdsByNewsId(Long newsId);

    void deleteByNewsId(Long newsId);

    List<Long> findNewsIdsByTeamId(Long teamId);
}