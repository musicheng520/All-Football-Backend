package com.msc.controller.user;

import com.msc.model.entity.Team;
import com.msc.result.Result;
import com.msc.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/follows")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    @PostMapping("/{teamId}")
    public Result<Void> follow(@PathVariable Long teamId) {
        followService.followTeam(teamId);
        return Result.success();
    }

    @DeleteMapping("/{teamId}")
    public Result<Void> unfollow(@PathVariable Long teamId) {
        followService.unfollowTeam(teamId);
        return Result.success();
    }

    @GetMapping("/me")
    public Result<List<Team>> myFollows() {
        return Result.success(followService.myFollowedTeams());
    }
}