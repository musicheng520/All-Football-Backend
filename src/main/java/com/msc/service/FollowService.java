package com.msc.service;

import com.msc.model.entity.Team;
import java.util.List;

public interface FollowService {

    void followTeam(Long teamId);

    void unfollowTeam(Long teamId);

    List<Team> myFollowedTeams();
}