package com.msc.mapper;

import com.msc.model.entity.Team;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TeamMapper {
    Team findById(Long id);

    void delete(Long id);

    void update(Team team);

    void insert(Team team);

    long count();

    List<Team> findPage(@Param("offset") int offset,
                        @Param("size") int size);
}
