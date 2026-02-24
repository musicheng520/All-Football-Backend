package com.msc.controller.user;

import com.msc.mapper.FixtureMapper;
import com.msc.model.entity.Fixture;
import com.msc.result.Result;
import com.msc.service.FixtureService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fixtures")
@RequiredArgsConstructor
public class FixtureController {
    private final FixtureService fixtureService;

    @GetMapping("/{id}")
    public Result<Fixture> getById(@PathVariable Long id) {
        return Result.success(fixtureService.getById(id));
    }
}
