package com.msc.mapper;

import com.msc.model.entity.Player;
import com.msc.model.vo.PlayerVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
@Mapper
public interface PlayerMapper {

    Integer existsById(Long id);

    Player findById(Long id);

    void insert(Player player);

    void update(Player player);

    void upsert(Player player);

    void delete(Long id);

    long count(@Param("teamId") Long teamId,
               @Param("season") Integer season);

    List<Player> findByIds(@Param("ids") List<Long> ids);

    long totalCount();

    List<Player> findPage(@Param("offset") int offset,
                          @Param("size") int size,
                          @Param("teamId") Long teamId,
                          @Param("season") Integer season);

    @Select("select *from players where team_id = #{teamId}")
    List<Player> findByTeamId(Long teamId);

    List<Player> searchByName(String name);

    List<PlayerVO> findPlayersWithStats(Long teamId, Integer season);
}