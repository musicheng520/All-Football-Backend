package com.msc.service;

import com.msc.model.dto.NewsCreateDTO;
import com.msc.model.entity.News;
import com.msc.model.vo.NewsDetailVO;
import com.msc.result.PageResult;

import java.util.List;

public interface NewsService {

    void create(NewsCreateDTO dto);

    News findById(Long id);

    List<News> findAll();

    void delete(Long id);

     List<News> findByPlayerId(Long playerId);

    List<News> findByTeamId(Long teamId);

    NewsDetailVO getDetail(Long id);

    PageResult<News> page(int page, int size);
}