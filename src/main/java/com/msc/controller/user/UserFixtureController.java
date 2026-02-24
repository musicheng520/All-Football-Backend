package com.msc.controller.user;

import com.msc.model.entity.Fixture;
import com.msc.result.PageResult;
import com.msc.result.Result;
import com.msc.service.FixtureService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/fixtures")
@RequiredArgsConstructor
public class UserFixtureController {

    private final FixtureService fixtureService;

    // 1. get by id
    @GetMapping("/{id}")
    public Result<Fixture> getById(@PathVariable Long id) {
        return Result.success(fixtureService.findById(id));
    }

    // pagination + query by params
    @GetMapping
    public Result<PageResult<Fixture>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long leagueId,
            @RequestParam(required = false) Integer season
    ) {
        return Result.success(
                fixtureService.page(page, size, leagueId, season)
        );
    }
}