package com.msc.service.impl;

import com.msc.mapper.FollowMapper;
import com.msc.mapper.TeamMapper;
import com.msc.model.entity.Follow;
import com.msc.model.entity.Team;
import com.msc.service.FollowService;
import com.msc.utils.ThreadLocalUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {

    private final FollowMapper followMapper;
    private final TeamMapper teamMapper;

    @Override
    public void followTeam(Long teamId) {

        Long userId = ThreadLocalUtil.get();

        // prevent duplicate follow
        Follow exists = followMapper.findByUserAndTeam(userId, teamId);
        if (exists != null) {
            return;
        }

        Follow follow = new Follow();
        follow.setUserId(userId);
        follow.setTeamId(teamId);
        follow.setCreatedAt(LocalDateTime.now());

        followMapper.insert(follow);
    }

    @Override
    public void unfollowTeam(Long teamId) {

        Long userId = ThreadLocalUtil.get();
        followMapper.deleteByUserAndTeam(userId, teamId);
    }

    @Override
    public List<Team> myFollowedTeams() {

        Long userId = ThreadLocalUtil.get();
        List<Long> teamIds = followMapper.findTeamIdsByUserId(userId);

        if (teamIds == null || teamIds.isEmpty()) {
            return List.of();
        }

        List<Team> result = new ArrayList<>();
        for (Long teamId : teamIds) {
            Team team = teamMapper.findById(teamId);
            if (team != null) {
                result.add(team);
            }
        }
        return result;
    }
}