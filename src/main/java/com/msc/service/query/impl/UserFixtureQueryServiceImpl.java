package com.msc.service.query.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msc.config.FootballProperties;
import com.msc.model.entity.Fixture;
import com.msc.model.vo.fixture.FixtureDetailVO;
import com.msc.result.PageResult;
import com.msc.service.ExternalFootballService;
import com.msc.service.FixtureService;
import com.msc.service.query.UserFixtureQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserFixtureQueryServiceImpl implements UserFixtureQueryService {

    private final FixtureService fixtureService;
    private final ExternalFootballService externalFootballService;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final FootballProperties footballProperties;

    /**
     * Fixture page query
     */
    @Override
    public PageResult<Fixture> page(int page,
                                    int size,
                                    Long leagueId,
                                    Integer season) {

        // current season → MySQL
        if (season == null || season.equals(footballProperties.getDefaultSeason())) {

            return fixtureService.page(page, size, leagueId, season);
        }

        String key = "fixtures:league:" + leagueId +
                ":season:" + season +
                ":page:" + page +
                ":size:" + size;

        try {

            // Redis
            String cache = stringRedisTemplate.opsForValue().get(key);

            if (cache != null) {

                System.out.println("[RedisHit] " + key);

                return objectMapper.readValue(
                        cache,
                        objectMapper.getTypeFactory()
                                .constructParametricType(PageResult.class, Fixture.class)
                );
            }

            System.out.println("[RedisMiss] " + key);

            // External API
            String json = externalFootballService.fetchFixturesBySeason(leagueId, season);

            JsonNode root = objectMapper.readTree(json);
            JsonNode response = root.get("response");

            List<Fixture> list = new ArrayList<>();

            if (response != null && response.isArray()) {

                for (JsonNode node : response) {

                    JsonNode fixtureNode = node.get("fixture");
                    JsonNode teamsNode = node.get("teams");
                    JsonNode goalsNode = node.get("goals");
                    JsonNode leagueNode = node.get("league");

                    if (fixtureNode == null || teamsNode == null) continue;

                    Fixture f = new Fixture();

                    // id
                    f.setId(fixtureNode.get("id").asLong());

                    f.setLeagueId(leagueId);
                    f.setSeason(season);

                    // round
                    if (leagueNode != null && leagueNode.get("round") != null) {
                        f.setRound(leagueNode.get("round").asText());
                    }

                    // home team
                    JsonNode homeTeam = teamsNode.get("home");

                    if (homeTeam != null) {

                        if (homeTeam.get("id") != null)
                            f.setHomeTeamId(homeTeam.get("id").asLong());

                        if (homeTeam.get("name") != null)
                            f.setHomeTeamName(homeTeam.get("name").asText());

                        if (homeTeam.get("logo") != null)
                            f.setHomeTeamLogo(homeTeam.get("logo").asText());
                    }

                    // away team
                    JsonNode awayTeam = teamsNode.get("away");

                    if (awayTeam != null) {

                        if (awayTeam.get("id") != null)
                            f.setAwayTeamId(awayTeam.get("id").asLong());

                        if (awayTeam.get("name") != null)
                            f.setAwayTeamName(awayTeam.get("name").asText());

                        if (awayTeam.get("logo") != null)
                            f.setAwayTeamLogo(awayTeam.get("logo").asText());
                    }

                    // score
                    if (goalsNode != null) {

                        if (goalsNode.get("home") != null && !goalsNode.get("home").isNull()) {
                            f.setHomeScore(goalsNode.get("home").asInt());
                        }

                        if (goalsNode.get("away") != null && !goalsNode.get("away").isNull()) {
                            f.setAwayScore(goalsNode.get("away").asInt());
                        }
                    }

                    // match time
                    if (fixtureNode.get("date") != null) {

                        try {

                            f.setMatchTime(
                                    OffsetDateTime
                                            .parse(fixtureNode.get("date").asText())
                                            .toLocalDateTime()
                            );

                        } catch (Exception ignored) {}
                    }

                    // status
                    JsonNode statusNode = fixtureNode.get("status");

                    if (statusNode != null) {

                        if (statusNode.get("short") != null) {
                            f.setStatus(statusNode.get("short").asText());
                        }

                        if (statusNode.get("elapsed") != null && !statusNode.get("elapsed").isNull()) {
                            f.setElapsed(statusNode.get("elapsed").asInt());
                        }
                    }

                    // referee
                    if (fixtureNode.get("referee") != null && !fixtureNode.get("referee").isNull()) {
                        f.setReferee(fixtureNode.get("referee").asText());
                    }

                    // venue
                    if (fixtureNode.get("venue") != null) {

                        JsonNode venueNode = fixtureNode.get("venue");

                        if (venueNode.get("name") != null) {
                            f.setVenue(venueNode.get("name").asText());
                        }
                    }

                    list.add(f);
                }
            }

            // 排序（非常重要）
            list.sort(
                    Comparator.comparing(Fixture::getMatchTime).reversed()
            );

            // 分页
            int start = (page - 1) * size;

            if (start >= list.size()) {

                PageResult<Fixture> empty =
                        new PageResult<>(list.size(), page, size, Collections.emptyList());

                return empty;
            }

            int end = Math.min(start + size, list.size());

            List<Fixture> pageList = list.subList(start, end);

            PageResult<Fixture> result =
                    new PageResult<>(list.size(), page, size, pageList);

            // Redis cache
            stringRedisTemplate.opsForValue()
                    .set(
                            key,
                            objectMapper.writeValueAsString(result),
                            Duration.ofHours(12)
                    );

            return result;

        } catch (Exception e) {

            throw new RuntimeException("Historical fixture query failed", e);
        }
    }

    /**
     * Fixture detail query
     */
    @Override
    public FixtureDetailVO getFixtureDetail(Long fixtureId) {

        String key = "fixture:detail:" + fixtureId;

        try {

            String cache = stringRedisTemplate.opsForValue().get(key);

            if (cache != null) {
                System.out.println("[RedisHit] " + key);
                return objectMapper.readValue(cache, FixtureDetailVO.class);
            }

            System.out.println("[RedisMiss] " + key);

            Fixture fixture = fixtureService.findById(fixtureId);

            FixtureDetailVO vo;

            // 当前赛季：数据库里能找到，并且 season = defaultSeason
            if (fixture != null
                    && fixture.getSeason() != null
                    && fixture.getSeason().equals(footballProperties.getDefaultSeason())) {

                vo = externalFootballService.buildFixtureDetailFromDb(fixtureId);

            } else {
                // 历史赛季：数据库找不到 or 非当前赛季
                vo = externalFootballService.fetchHistoricalFixtureDetail(fixtureId);
            }

            if (vo != null) {
                stringRedisTemplate.opsForValue()
                        .set(key,
                                objectMapper.writeValueAsString(vo),
                                Duration.ofHours(12));
            }

            return vo;

        } catch (Exception e) {
            throw new RuntimeException("Fixture detail query failed", e);
        }
    }
}