package com.msc.mapper;

import com.msc.model.entity.Lineup;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface LineupMapper {

    void deleteByFixtureId(Long fixtureId);

    void insert(Lineup lineup);

    void insertBatch(List<Lineup> list);
}