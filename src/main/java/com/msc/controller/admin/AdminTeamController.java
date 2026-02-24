package com.msc.controller.admin;

import com.msc.model.entity.Team;
import com.msc.result.Result;
import com.msc.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/teams")
@RequiredArgsConstructor
public class AdminTeamController {

    private final TeamService teamService;

    @PostMapping
    public Result<Void> create(@RequestBody Team team) {
        teamService.create(team);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id,
                               @RequestBody Team team) {
        team.setId(id);
        teamService.update(team);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        teamService.delete(id);
        return Result.success();
    }

}
