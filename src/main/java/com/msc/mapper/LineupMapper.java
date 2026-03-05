package com.msc.mapper;

import com.msc.model.entity.Lineup;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LineupMapper {

    void deleteByFixtureId(Long fixtureId);

    void insert(Lineup lineup);
}