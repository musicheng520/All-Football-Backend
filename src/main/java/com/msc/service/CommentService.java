package com.msc.service;

import com.msc.model.dto.CommentCreateDTO;
import com.msc.model.entity.Comment;
import com.msc.model.vo.CommentVO;
import com.msc.result.PageResult;

import java.util.List;

public interface CommentService {

    void create(CommentCreateDTO dto);

    List<CommentVO> findByNewsId(Long newsId);

    void delete(Long id);

    PageResult<Comment> page(int page, int size);
}