package com.msc.mapper;

import com.msc.model.entity.Fixture;
import com.msc.model.entity.Lineup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface LineupMapper {

    void deleteByFixtureId(Long fixtureId);

    void insert(Lineup lineup);

    void insertBatch(List<Lineup> list);

    @Select("SELECT * FROM lineups WHERE fixture_id = #{fixtureId}")
    List<Lineup> findByFixtureId(Long fixtureId);
}