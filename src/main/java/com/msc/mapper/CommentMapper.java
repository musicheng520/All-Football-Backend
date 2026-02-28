package com.msc.mapper;

import com.msc.model.entity.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CommentMapper {

    void insert(Comment comment);

    List<Comment> findByNewsId(Long newsId);

    void delete(Long id);

    long count();

    List<Comment> page(@Param("offset") int offset,
                       @Param("size") int size);
}