package com.msc.controller.admin;

import com.msc.model.dto.NewsCreateDTO;
import com.msc.model.entity.News;
import com.msc.result.Result;
import com.msc.service.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/news")
@RequiredArgsConstructor
public class AdminNewsController {

    private final NewsService newsService;

    @PostMapping
    public Result<Void> create(@RequestBody NewsCreateDTO dto) {
        newsService.create(dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        newsService.delete(id);
        return Result.success();
    }

    @GetMapping
    public Result<List<News>> list() {
        return Result.success(newsService.findAll());
    }
}