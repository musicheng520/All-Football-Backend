package com.msc.mapper;

import com.msc.model.entity.Comment;
import com.msc.model.vo.CommentVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CommentMapper {

    void insert(Comment comment);

    List<CommentVO> findByNewsId(Long newsId);



    void delete(Long id);

    long count();

    List<Comment> page(@Param("offset") int offset,
                       @Param("size") int size);
}