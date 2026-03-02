package com.msc.mapper;

import com.msc.model.entity.Fixture;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FixtureMapper {

    Fixture findById(Long id);

    void insert(Fixture fixture);

    long totalCount();

    void upsert(Fixture fixture);

    void update(Fixture fixture);

    void delete(Long id);

    long count(@Param("leagueId") Long leagueId,
               @Param("season") Integer season);

    List<Fixture> findPage(@Param("offset") int offset,
                           @Param("size") int size,
                           @Param("leagueId") Long leagueId,
                           @Param("season") Integer season);

    void updateScore(@Param("id") Long id,
                     @Param("homeScore") Integer homeScore,
                     @Param("awayScore") Integer awayScore,
                     @Param("elapsed") Integer elapsed,
                     @Param("status") String status);
}