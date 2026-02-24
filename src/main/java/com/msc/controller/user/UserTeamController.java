package com.msc.controller.user;

import com.msc.model.entity.Team;
import com.msc.result.PageResult;
import com.msc.result.Result;
import com.msc.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/teams")
@RequiredArgsConstructor
public class UserTeamController {

    private final TeamService teamService;

    // find by id
    @GetMapping("/{id}")
    public Result<Team> getById(@PathVariable Long id) {
        return Result.success(teamService.findById(id));
    }

    // pagination
    @GetMapping
    public Result<PageResult<Team>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return Result.success(teamService.page(page, size));
    }
}