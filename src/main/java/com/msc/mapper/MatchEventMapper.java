package com.msc.mapper;

import com.msc.model.entity.MatchEvent;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MatchEventMapper {

    void deleteByFixtureId(Long fixtureId);

    void insertBatch(List<MatchEvent> list);
}