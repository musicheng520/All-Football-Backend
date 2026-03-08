package com.msc.controller.user;

import com.msc.model.entity.Player;
import com.msc.model.vo.PlayerDetailVO;
import com.msc.result.PageResult;
import com.msc.result.Result;
import com.msc.service.query.PlayerQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/players")
@RequiredArgsConstructor
public class UserPlayerController {

    private final PlayerQueryService playerQueryService;

    // player detail
    @GetMapping("/{id}")
    public Result<PlayerDetailVO> detail(
            @PathVariable Long id,
            @RequestParam Integer season
    ) {
        return Result.success(
                playerQueryService.getPlayerDetail(id, season)
        );
    }

    // player list
    @GetMapping
    public Result<PageResult<Player>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam Long teamId,
            @RequestParam Integer season
    ) {
        return Result.success(
                playerQueryService.getPlayerList(page, size, teamId, season)
        );
    }
}