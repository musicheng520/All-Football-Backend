package com.msc.controller.user;

import com.msc.model.entity.Player;
import com.msc.result.PageResult;
import com.msc.result.Result;
import com.msc.service.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/players")
@RequiredArgsConstructor
public class UserPlayerController {

    private final PlayerService playerService;

    // 1️ get by id
    @GetMapping("/{id}")
    public Result<Player> getById(@PathVariable Long id) {
        return Result.success(playerService.findById(id));
    }

    // 2. pagination & get by params
    @GetMapping
    public Result<PageResult<Player>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long teamId,
            @RequestParam(required = false) Integer season
    ) {
        return Result.success(
                playerService.page(page, size, teamId, season)
        );
    }
}