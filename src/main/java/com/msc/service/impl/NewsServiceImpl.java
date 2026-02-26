package com.msc.service.impl;

import com.msc.mapper.NewsMapper;
import com.msc.mapper.NewsPlayerMapper;
import com.msc.mapper.NewsTeamMapper;
import com.msc.model.dto.NewsCreateDTO;
import com.msc.model.entity.News;
import com.msc.model.entity.NewsPlayer;
import com.msc.model.entity.NewsTeam;
import com.msc.service.NewsService;
import com.msc.utils.ThreadLocalUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NewsServiceImpl implements NewsService {

    private final NewsMapper newsMapper;
    private final NewsTeamMapper newsTeamMapper;
    private final NewsPlayerMapper newsPlayerMapper;

    @Override
    @Transactional
    public void create(NewsCreateDTO dto) {

        Long authorId = ThreadLocalUtil.get();

        // 1. create news
        News news = new News();
        news.setTitle(dto.getTitle());
        news.setContent(dto.getContent());
        news.setCategory(dto.getCategory());
        news.setAuthorId(authorId);
        news.setPublishedAt(LocalDateTime.now());
        news.setCreatedAt(LocalDateTime.now());
        news.setUpdatedAt(LocalDateTime.now());

        // 2. insert news（automatic generate id）
        newsMapper.insert(news);

        Long newsId = news.getId();

        // 3. connect to teams
        if (dto.getTeamIds() != null && !dto.getTeamIds().isEmpty()) {

            List<NewsTeam> teamList = new ArrayList<>();

            for (Long teamId : dto.getTeamIds()) {
                NewsTeam nt = new NewsTeam();
                nt.setNewsId(newsId);
                nt.setTeamId(teamId);
                teamList.add(nt);
            }

            newsTeamMapper.batchInsert(teamList);
        }

        // 4. connect to player
        if (dto.getPlayerIds() != null && !dto.getPlayerIds().isEmpty()) {

            List<NewsPlayer> playerList = new ArrayList<>();

            for (Long playerId : dto.getPlayerIds()) {
                NewsPlayer np = new NewsPlayer();
                np.setNewsId(newsId);
                np.setPlayerId(playerId);
                playerList.add(np);
            }

            newsPlayerMapper.batchInsert(playerList);
        }
    }

    @Override
    public News findById(Long id) {
        return newsMapper.findById(id);
    }

    @Override
    public List<News> findAll() {
        return newsMapper.findAll();
    }

    @Override
    @Transactional
    public void delete(Long id) {

        newsTeamMapper.deleteByNewsId(id);
        newsPlayerMapper.deleteByNewsId(id);
        newsMapper.delete(id);
    }

    public List<News> findByTeamId(Long teamId) {

        List<Long> newsIds = newsTeamMapper.findNewsIdsByTeamId(teamId);

        if (newsIds == null || newsIds.isEmpty()) {
            return List.of();
        }

        return newsMapper.findByIds(newsIds);
    }

    public List<News> findByPlayerId(Long playerId) {

        List<Long> newsIds = newsPlayerMapper.findNewsIdsByPlayerId(playerId);

        if (newsIds == null || newsIds.isEmpty()) {
            return List.of();
        }

        return newsMapper.findByIds(newsIds);
    }
}