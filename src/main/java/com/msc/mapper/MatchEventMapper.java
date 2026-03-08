package com.msc.mapper;

import com.msc.model.entity.MatchEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MatchEventMapper {

    void deleteByFixtureId(Long fixtureId);

    void insertBatch(List<MatchEvent> list);

    @Select("SELECT *\n" +
            "FROM match_events\n" +
            "WHERE fixture_id = #{fixtureId}\n" +
            "ORDER BY minute ASC")
    List<MatchEvent> findByFixtureId(Long fixtureId);
}