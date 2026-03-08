package com.msc.controller.user;

import com.msc.model.entity.Fixture;
import com.msc.model.vo.fixture.FixtureDetailVO;
import com.msc.result.PageResult;
import com.msc.result.Result;
import com.msc.service.FixtureService;
import com.msc.service.UserFixtureQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/fixtures")
@RequiredArgsConstructor
public class UserFixtureController {

    private final FixtureService fixtureService;
    private final UserFixtureQueryService userFixtureQueryService;

    // pagination + query by params
    @GetMapping
    public Result<PageResult<Fixture>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long leagueId,
            @RequestParam(required = false) Integer season
    ) {

        return Result.success(
                userFixtureQueryService.page(page, size, leagueId, season)
        );
    }

    @GetMapping("/{id}")
    public Result<FixtureDetailVO> getDetail(@PathVariable Long id) {

        return Result.success(
                userFixtureQueryService.getFixtureDetail(id)
        );
    }
}