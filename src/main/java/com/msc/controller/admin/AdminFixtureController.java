package com.msc.controller.admin;

import com.msc.model.entity.Fixture;
import com.msc.result.Result;
import com.msc.service.FixtureService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/fixtures")
@RequiredArgsConstructor
public class AdminFixtureController {

    private final FixtureService fixtureService;

    @PostMapping
    public Result<Void> create(@RequestBody Fixture fixture) {
        fixtureService.create(fixture);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id,
                               @RequestBody Fixture fixture) {
        fixture.setId(id);
        fixtureService.update(fixture);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        fixtureService.delete(id);
        return Result.success();
    }

    // prepare for future updating
    @PatchMapping("/{id}/score")
    public Result<Void> updateScore(@PathVariable Long id,
                                    @RequestParam Integer homeScore,
                                    @RequestParam Integer awayScore,
                                    @RequestParam(required = false) Integer elapsed,
                                    @RequestParam(required = false) String status) {

        fixtureService.updateScore(id, homeScore, awayScore, elapsed, status);
        return Result.success();
    }
}