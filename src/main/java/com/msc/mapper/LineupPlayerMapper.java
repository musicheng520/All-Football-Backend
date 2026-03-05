package com.msc.mapper;

import com.msc.model.entity.LineupPlayer;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface LineupPlayerMapper {

    void deleteByFixtureId(Long fixtureId);

    void insertBatch(List<LineupPlayer> list);
}