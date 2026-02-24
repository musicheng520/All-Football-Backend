package com.msc.mapper;

import com.msc.model.entity.Player;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
@Mapper
public interface PlayerMapper {

    Player findById(Long id);

    void insert(Player player);

    void update(Player player);

    void delete(Long id);

    long count(@Param("teamId") Long teamId,
               @Param("season") Integer season);

    List<Player> findPage(@Param("offset") int offset,
                          @Param("size") int size,
                          @Param("teamId") Long teamId,
                          @Param("season") Integer season);
}