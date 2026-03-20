package com.msc.service.query.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msc.config.FootballProperties;
import com.msc.model.entity.*;
import com.msc.model.vo.PlayerVO;
import com.msc.model.vo.TeamDetailVO;
import com.msc.result.PageResult;
import com.msc.mapper.*;
import com.msc.service.ExternalFootballService;
import com.msc.service.PlayerProfileService;
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
    private final PlayerProfileService playerProfileService;

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

            List<Team> teams = teamMapper.findByLeagueAndSeason(leagueId, season);

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
    public PageResult<Team> searchByName(String name, int page, int size) {

        List<Team> teams = teamMapper.searchByName(name);

        int start = (page - 1) * size;
        int end = Math.min(start + size, teams.size());

        List<Team> pageList = teams.subList(start, end);

        PageResult<Team> result = new PageResult<>();
        result.setTotal(teams.size());
        result.setPage(page);
        result.setSize(size);
        result.setRecords(pageList);

        return result;
    }

    private void enrichPlayerProfile(List<PlayerVO> squad) {

        if (squad == null || squad.isEmpty()) return;

        for (PlayerVO p : squad) {

            if (p.getId() == null) continue;

            // 已经有完整信息就跳过
            boolean hasProfile =
                    p.getNationality() != null &&
                            p.getPosition() != null &&
                            p.getNumber() != null;

            if (hasProfile) continue;

            try {

                PlayerProfile profile =
                        playerProfileService.getProfileByPlayerId(p.getId());

                if (profile != null) {

                    if (p.getNationality() == null) {
                        p.setNationality(profile.getNationality());
                    }

                    if (p.getPosition() == null) {
                        p.setPosition(profile.getPosition());
                    }

                    if (p.getNumber() == null) {
                        p.setNumber(profile.getNumber());
                    }
                }

            } catch (Exception ignored) {
                // ❗不能影响主流程
            }
        }
    }

    @Override
    public TeamDetailVO getTeamDetail(Long teamId, Integer season) {

        // ---------- 当前赛季 ----------
        if (season.equals(footballProperties.getDefaultSeason())) {

            Team team = teamMapper.findById(teamId);

            List<PlayerVO> squad =
                    playerMapper.findPlayersWithStats(teamId, season);

            //  补 profile（关键）
            enrichPlayerProfile(squad);

            List<Fixture> fixtures =
                    fixtureMapper.findByTeamId(teamId);

            TeamDetailVO vo = new TeamDetailVO();
            vo.setTeam(team);
            vo.setSquad(squad);
            vo.setFixtures(fixtures);

            return vo;
        }

        // ---------- 历史赛季 ----------
        String key = "team:detail:" + teamId + ":" + season;

        try {

            String cache = redisTemplate.opsForValue().get(key);

            if (cache != null) {

                TeamDetailVO vo =
                        objectMapper.readValue(cache, TeamDetailVO.class);

                // 🔥 Redis反序列化后再补一层（防缓存污染）
                enrichPlayerProfile(vo.getSquad());

                return vo;
            }

        } catch (Exception ignored) {}

        // ---------- Redis miss → API ----------
        TeamDetailVO vo =
                externalFootballService.fetchTeamDetail(teamId, season);

        //  再补一层（保证一致）
        enrichPlayerProfile(vo.getSquad());

        // ---------- 写缓存 ----------
        try {

            redisTemplate.opsForValue().set(
                    key,
                    objectMapper.writeValueAsString(vo),
                    Duration.ofHours(2) // 建议缩短
            );

        } catch (Exception ignored) {}

        return vo;
    }
}