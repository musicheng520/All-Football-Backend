package com.msc.controller.user;

import com.msc.model.entity.News;
import com.msc.model.vo.NewsDetailVO;
import com.msc.result.Result;
import com.msc.service.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/news")
@RequiredArgsConstructor
public class UserNewsController {

    private final NewsService newsService;

    @GetMapping
    public Result<List<News>> list() {
        return Result.success(newsService.findAll());
    }

    @GetMapping("/{id}")
    public Result<NewsDetailVO> detail(@PathVariable Long id) {
        return Result.success(newsService.getDetail(id));
    }

    @GetMapping("/team/{teamId}")
    public Result<List<News>> byTeam(@PathVariable Long teamId) {
        return Result.success(newsService.findByTeamId(teamId));
    }

    @GetMapping("/player/{playerId}")
    public Result<List<News>> byPlayer(@PathVariable Long playerId) {
        return Result.success(newsService.findByPlayerId(playerId));
    }
}