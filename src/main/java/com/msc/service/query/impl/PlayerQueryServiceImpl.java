package com.msc.service.query.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msc.config.FootballProperties;
import com.msc.mapper.PlayerMapper;
import com.msc.mapper.PlayerStatsMapper;
import com.msc.mapper.TeamMapper;
import com.msc.model.entity.Player;
import com.msc.model.entity.PlayerProfile;
import com.msc.model.entity.PlayerStats;
import com.msc.model.entity.Team;
import com.msc.model.vo.PlayerDetailVO;
import com.msc.result.PageResult;
import com.msc.service.ExternalFootballService;
import com.msc.service.PlayerProfileService;
import com.msc.service.query.PlayerQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlayerQueryServiceImpl implements PlayerQueryService {

    private final PlayerMapper playerMapper;
    private final PlayerStatsMapper playerStatsMapper;
    private final TeamMapper teamMapper;
    private final ExternalFootballService externalFootballService;
    private final PlayerProfileService playerProfileService;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final FootballProperties footballProperties;

    // ===============================
    // player list
    // ===============================
    @Override
    public PageResult<Player> getPlayerList(int page, int size, Long teamId, Integer season) {

        String key = "players:" + teamId + ":" + season + ":" + page + ":" + size;

        try {

            String cache = redisTemplate.opsForValue().get(key);

            if (cache != null) {

                System.out.println("[RedisHit] " + key);

                return objectMapper.readValue(
                        cache,
                        objectMapper.getTypeFactory()
                                .constructParametricType(PageResult.class, Player.class)
                );
            }

        } catch (Exception ignored) {}

        System.out.println("[RedisMiss] " + key);

        PageResult<Player> result;

        // current season → MySQL
        if (season.equals(footballProperties.getDefaultSeason())) {

            List<Player> players = playerMapper.findByTeamId(teamId);

            int start = (page - 1) * size;
            int end = Math.min(start + size, players.size());

            List<Player> pageList = players.subList(start, end);

            result = new PageResult<>();
            result.setTotal(players.size());
            result.setPage(page);
            result.setSize(size);
            result.setRecords(pageList);

        }
        // history → API
        else {

            result = externalFootballService.fetchPlayersForQuery(
                    teamId,
                    season,
                    page,
                    size
            );

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
    public PageResult<Player> searchByName(String name, int page, int size) {

        List<Player> players = playerMapper.searchByName(name);

        int start = (page - 1) * size;
        int end = Math.min(start + size, players.size());

        List<Player> pageList = players.subList(start, end);

        PageResult<Player> result = new PageResult<>();
        result.setTotal(players.size());
        result.setPage(page);
        result.setSize(size);
        result.setRecords(pageList);

        return result;
    }

    // ===============================
    // player detail
    // ===============================
    @Override
    public PlayerDetailVO getPlayerDetail(Long playerId, Integer season) {

        String key = "player:detail:" + playerId + ":" + season;

        try {

            String cache = redisTemplate.opsForValue().get(key);

            if (cache != null) {

                System.out.println("[RedisHit] " + key);

                PlayerDetailVO vo =
                        objectMapper.readValue(cache, PlayerDetailVO.class);

                // 👇 关键：cache出来也要补 profile
                attachProfile(vo);

                return vo;
            }

        } catch (Exception ignored) {}

        System.out.println("[RedisMiss] " + key);

        PlayerDetailVO vo;

        // current season → MySQL
        if (season.equals(footballProperties.getDefaultSeason())) {

            Player player = playerMapper.findById(playerId);
            Team team = teamMapper.findById(player.getTeamId());

            List<PlayerStats> stats =
                    playerStatsMapper.findByPlayerAndSeason(playerId, season);

            vo = new PlayerDetailVO();
            vo.setPlayer(player);
            vo.setTeam(team);
            vo.setStatistics(stats);

        }
        // history → API
        else {

            vo = externalFootballService.fetchPlayerDetail(playerId, season);

            try {

                redisTemplate.opsForValue().set(
                        key,
                        objectMapper.writeValueAsString(vo),
                        Duration.ofHours(6)
                );

            } catch (Exception ignored) {}
        }

        // 核心：统一补 profile
        attachProfile(vo);

        return vo;
    }

    private void attachProfile(PlayerDetailVO vo) {

        if (vo == null || vo.getPlayer() == null) {
            return;
        }

        try {

            PlayerProfile profile =
                    playerProfileService.getProfileByPlayerId(vo.getPlayer().getId());

            vo.setProfile(profile);

        } catch (Exception e) {

            // 不影响主流程（非常重要）
            System.out.println("[ProfileFallback] " + e.getMessage());
        }
    }
}