package com.msc.controller.admin;

import com.msc.model.dto.NewsCreateDTO;
import com.msc.model.entity.News;
import com.msc.result.PageResult;
import com.msc.result.Result;
import com.msc.service.NewsService;
import com.msc.utils.S3Util;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/admin/news")
@RequiredArgsConstructor
public class AdminNewsController {

    private final NewsService newsService;
    private final S3Util s3Util;

    @PostMapping("/create")
    public Result<Void> create(
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam("category") String category,
            @RequestParam(value = "cover", required = false) MultipartFile cover,
            @RequestParam(value = "images", required = false) List<MultipartFile> images
    ) throws Exception {

        NewsCreateDTO dto = new NewsCreateDTO();

        dto.setTitle(title);
        dto.setContent(content);
        dto.setCategory(category);

        // 1️. 上传封面
        if (cover != null && !cover.isEmpty()) {
            String coverUrl = s3Util.uploadNewsCover(cover);
            dto.setCoverImage(coverUrl);
        }

        // 2️. 上传多图
        if (images != null && !images.isEmpty()) {

            List<String> urls = new ArrayList<>();

            for (MultipartFile file : images) {
                String url = s3Util.uploadNewsContent(file);
                urls.add(url);
            }

            dto.setImages(String.join(",", urls));
        }

        // 3️ 调用 service
        newsService.create(dto);

        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        newsService.delete(id);
        return Result.success();
    }

    @GetMapping
    public Result<PageResult<News>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return Result.success(newsService.page(page, size));
    }
}