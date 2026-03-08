package com.msc.service.query.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msc.config.FootballProperties;
import com.msc.model.entity.*;
import com.msc.model.vo.TeamDetailVO;
import com.msc.result.PageResult;
import com.msc.mapper.*;
import com.msc.service.ExternalFootballService;
import com.msc.service.query.TeamQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TeamQueryServiceImpl implements TeamQueryService {

    private final TeamMapper teamMapper;
    private final PlayerMapper playerMapper;
    private final FixtureMapper fixtureMapper;
    private final ExternalFootballService externalFootballService;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final FootballProperties footballProperties;

    @Override
    public PageResult<Team> getTeamList(int page, int size, Long leagueId, Integer season) {

        String key = "teams:" + leagueId + ":" + season + ":" + page + ":" + size;

        // 只有历史赛季才查 Redis
        if (!season.equals(footballProperties.getDefaultSeason())) {

            try {
                String cache = redisTemplate.opsForValue().get(key);

                if (cache != null) {
                    return objectMapper.readValue(
                            cache,
                            objectMapper.getTypeFactory()
                                    .constructParametricType(PageResult.class, Team.class)
                    );
                }

            } catch (Exception ignored) {}
        }

        PageResult<Team> result;

        // 当前赛季 → MySQL
        if (season.equals(footballProperties.getDefaultSeason())) {

            List<Team> teams = teamMapper.findBySeason(season);

            int start = (page - 1) * size;
            int end = Math.min(start + size, teams.size());

            List<Team> pageList = teams.subList(start, end);

            result = new PageResult<>();
            result.setTotal(teams.size());
            result.setPage(page);
            result.setSize(size);
            result.setRecords(pageList);

        } else {

            // 历史赛季 → API
            result = externalFootballService.fetchTeamsForQuery(leagueId, season, page, size);

            // 写 Redis
            try {

                redisTemplate.opsForValue().set(
                        key,
                        objectMapper.writeValueAsString(result),
                        Duration.ofHours(6)
                );

            } catch (Exception ignored) {}
        }

        return result;
    }

    @Override
    public TeamDetailVO getTeamDetail(Long teamId, Integer season) {

        String key = "team:detail:" + teamId + ":" + season;

        // 1 Redis
        try {
            String cache = redisTemplate.opsForValue().get(key);

            if (cache != null) {
                System.out.println("[RedisHit] " + key);
                return objectMapper.readValue(cache, TeamDetailVO.class);
            }

        } catch (Exception ignored) {}

        System.out.println("[RedisMiss] " + key);

        TeamDetailVO vo;

        // 2 current season -> MySQL
        if (season.equals(footballProperties.getDefaultSeason())) {

            Team team = teamMapper.findById(teamId);

            List<Player> squad = playerMapper.findByTeamId(teamId);

            List<Fixture> fixtures = fixtureMapper.findRecentByTeamId(teamId);

            vo = new TeamDetailVO();
            vo.setTeam(team);
            vo.setSquad(squad);
            vo.setRecentFixtures(fixtures);

        }
        // 3 history season -> API
        else {

            vo = externalFootballService.fetchTeamDetail(teamId, season);

            try {
                redisTemplate.opsForValue().set(
                        key,
                        objectMapper.writeValueAsString(vo),
                        Duration.ofHours(6)
                );
            } catch (Exception ignored) {}
        }

        return vo;
    }
}