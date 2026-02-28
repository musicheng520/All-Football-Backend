package com.msc.service.impl;

import com.msc.mapper.*;
import com.msc.model.dto.NewsCreateDTO;
import com.msc.model.entity.*;
import com.msc.model.vo.CommentVO;
import com.msc.model.vo.NewsDetailVO;
import com.msc.model.vo.PlayerSimpleVO;
import com.msc.model.vo.TeamSimpleVO;
import com.msc.result.PageResult;
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
    private final TeamMapper teamMapper;
    private final UserMapper userMapper;
    private final PlayerMapper playerMapper;
    private final CommentMapper commentMapper;

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

    @Override
    public NewsDetailVO getDetail(Long id) {

        // query news
        News news = newsMapper.findById(id);
        if (news == null) {
            throw new RuntimeException("News not found");
        }

        // query author
        User author = userMapper.findById(news.getAuthorId());

        // query relate teamIds
        List<Long> teamIds = newsTeamMapper.findTeamIdsByNewsId(id);
        List<Team> teams = teamIds.isEmpty()
                ? List.of()
                : teamMapper.findByIds(teamIds);

        // query relate playerIds
        List<Long> playerIds = newsPlayerMapper.findPlayerIdsByNewsId(id);
        List<Player> players = playerIds.isEmpty()
                ? List.of()
                : playerMapper.findByIds(playerIds);

        // query comments
        List<Comment> comments = commentMapper.findByNewsId(id);

        // assemble VO
        NewsDetailVO vo = new NewsDetailVO();
        vo.setId(news.getId());
        vo.setTitle(news.getTitle());
        vo.setContent(news.getContent());
        vo.setAuthorName(author.getUsername());
        vo.setCategory(news.getCategory());
        vo.setPublishedAt(news.getPublishedAt());

        // team conversion
        List<TeamSimpleVO> teamVOs = teams.stream().map(team -> {
            TeamSimpleVO t = new TeamSimpleVO();
            t.setId(team.getId());
            t.setName(team.getName());
            t.setLogo(team.getLogo());
            return t;
        }).toList();

        // player conversion
        List<PlayerSimpleVO> playerVOs = players.stream().map(player -> {
            PlayerSimpleVO p = new PlayerSimpleVO();
            p.setId(player.getId());
            p.setName(player.getName());
            p.setPhoto(player.getPhoto());
            return p;
        }).toList();

        // comment conversion（with username）
        List<CommentVO> commentVOs = comments.stream().map(comment -> {
            User user = userMapper.findById(comment.getUserId());
            CommentVO c = new CommentVO();
            c.setId(comment.getId());
            c.setContent(comment.getContent());
            c.setCreatedAt(comment.getCreatedAt());
            c.setUsername(user.getUsername());
            return c;
        }).toList();

        vo.setTeams(teamVOs);
        vo.setPlayers(playerVOs);
        vo.setComments(commentVOs);

        return vo;
    }

    @Override
    public PageResult<News> page(int page, int size) {

        int offset = (page - 1) * size;

        long total = newsMapper.count();

        List<News> records = newsMapper.page(offset, size);

        return new PageResult<>(
                total,
                page,
                size,
                records
        );
    }

    public List<News> findByPlayerId(Long playerId) {

        List<Long> newsIds = newsPlayerMapper.findNewsIdsByPlayerId(playerId);

        if (newsIds == null || newsIds.isEmpty()) {
            return List.of();
        }

        return newsMapper.findByIds(newsIds);
    }
}