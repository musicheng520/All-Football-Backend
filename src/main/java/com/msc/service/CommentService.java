package com.msc.service;

import com.msc.model.dto.CommentCreateDTO;
import com.msc.model.entity.Comment;

import java.util.List;

public interface CommentService {

    void create(CommentCreateDTO dto);

    List<Comment> findByNewsId(Long newsId);

    void delete(Long id);
}