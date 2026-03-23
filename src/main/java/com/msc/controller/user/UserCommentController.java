package com.msc.controller.user;

import com.msc.model.dto.CommentCreateDTO;
import com.msc.model.vo.CommentVO;
import com.msc.result.Result;
import com.msc.model.entity.Comment;
import com.msc.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
public class UserCommentController {

    private final CommentService commentService;

    @PostMapping
    public Result<Void> create(@RequestBody CommentCreateDTO dto) {
        commentService.create(dto);
        return Result.success();
    }

    @GetMapping("/news/{newsId}")
    public Result<List<CommentVO>> list(@PathVariable Long newsId) {
        return Result.success(commentService.findByNewsId(newsId));
    }
}