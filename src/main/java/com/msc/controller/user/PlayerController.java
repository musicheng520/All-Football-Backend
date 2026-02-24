package com.msc.controller.user;

import com.msc.model.entity.Player;
import com.msc.result.Result;
import com.msc.service.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/players")
@RequiredArgsConstructor
public class PlayerController {

    private final PlayerService playerService;
    @GetMapping("/{id}")
    public Result<Player> getById(@PathVariable Long id) {
        return Result.success(playerService.findById(id));
    }
}
