package com.msc.mapper;

import com.msc.model.entity.LineupPlayer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface LineupPlayerMapper {

    void deleteByFixtureId(Long fixtureId);

    void insertBatch(List<LineupPlayer> list);

    @Select("SELECT lp.*\n" +
            "FROM lineup_players lp\n" +
            "JOIN lineups l ON lp.lineup_id = l.id\n" +
            "WHERE l.fixture_id = #{fixtureId}")
    List<LineupPlayer> findByFixtureId(Long fixtureId);
}