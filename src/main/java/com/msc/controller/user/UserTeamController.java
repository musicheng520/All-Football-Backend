package com.msc.controller.user;

import com.msc.model.entity.Team;
import com.msc.model.vo.TeamDetailVO;
import com.msc.result.PageResult;
import com.msc.result.Result;
import com.msc.service.query.TeamQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/teams")
@RequiredArgsConstructor
public class UserTeamController {

    private final TeamQueryService teamQueryService;

    // team detail
// team detail
    @GetMapping("/{id}")
    public Result<TeamDetailVO> detail(
            @PathVariable Long id,
            @RequestParam Integer season
    ) {
        return Result.success(
                teamQueryService.getTeamDetail(id, season)
        );
    }

    // team list
    @GetMapping
    public Result<PageResult<Team>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam Long leagueId,
            @RequestParam Integer season
    ) {
        return Result.success(
                teamQueryService.getTeamList(page, size, leagueId, season)
        );
    }

    @GetMapping("/search")
    public Result<PageResult<Team>> search(
            @RequestParam String name,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return Result.success(
                teamQueryService.searchByName(name, page, size)
        );
    }
}