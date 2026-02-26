package com.msc.service.impl;

import com.msc.mapper.CommentMapper;
import com.msc.model.dto.CommentCreateDTO;
import com.msc.model.entity.Comment;
import com.msc.service.CommentService;
import com.msc.utils.ThreadLocalUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentMapper commentMapper;

    @Override
    public void create(CommentCreateDTO dto) {

        Long userId = ThreadLocalUtil.get();

        Comment comment = new Comment();
        comment.setNewsId(dto.getNewsId());
        comment.setUserId(userId);
        comment.setContent(dto.getContent());
        comment.setCreatedAt(LocalDateTime.now());
        comment.setUpdatedAt(LocalDateTime.now());

        commentMapper.insert(comment);
    }

    @Override
    public List<Comment> findByNewsId(Long newsId) {
        return commentMapper.findByNewsId(newsId);
    }

    @Override
    public void delete(Long id) {
        commentMapper.delete(id);
    }
}