package com.msc.mapper;

import com.msc.model.entity.MatchStatistic;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MatchStatisticMapper {

    void deleteByFixtureId(Long fixtureId);

    void insertBatch(List<MatchStatistic> list);

    @Select("SELECT *\n" +
            "FROM match_statistics\n" +
            "WHERE fixture_id = #{fixtureId}")
    List<MatchStatistic> findByFixtureId(Long fixtureId);
}