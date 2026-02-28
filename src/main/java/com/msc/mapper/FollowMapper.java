package com.msc.mapper;

import com.msc.model.entity.Follow;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface FollowMapper {

    void insert(Follow follow);

    long totalCount();

    void deleteByUserAndTeam(Long userId, Long teamId);

    List<Long> findTeamIdsByUserId(Long userId);

    Follow findByUserAndTeam(Long userId, Long teamId);
}