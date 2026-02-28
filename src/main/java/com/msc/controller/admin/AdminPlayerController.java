package com.msc.controller.admin;

import com.msc.model.entity.Player;
import com.msc.result.PageResult;
import com.msc.result.Result;
import com.msc.service.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/players")
@RequiredArgsConstructor
public class AdminPlayerController {

    private final PlayerService playerService;

    @PostMapping
    public Result<Void> create(@RequestBody Player player) {
        playerService.create(player);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id,
                               @RequestBody Player player) {
        player.setId(id);
        playerService.update(player);
        return Result.success();
    }

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

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        playerService.delete(id);
        return Result.success();
    }
}