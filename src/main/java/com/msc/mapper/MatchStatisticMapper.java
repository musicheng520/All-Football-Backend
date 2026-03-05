package com.msc.mapper;

import com.msc.model.entity.MatchStatistic;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MatchStatisticMapper {

    void deleteByFixtureId(Long fixtureId);

    void insertBatch(List<MatchStatistic> list);
}