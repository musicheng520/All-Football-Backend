package com.msc.mapper;
import com.msc.model.entity.News;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface NewsMapper {

    void insert(News news);

    News findById(Long id);

    List<News> findAll();

    void delete(Long id);

    List<News> findByIds(List<Long> ids);

    long count();

    List<News> page(int offset, int size);
}