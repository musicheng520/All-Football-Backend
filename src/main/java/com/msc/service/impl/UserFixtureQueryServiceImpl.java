package com.msc.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msc.model.entity.Fixture;
import com.msc.result.PageResult;
import com.msc.service.ExternalFootballService;
import com.msc.service.FixtureService;
import com.msc.service.UserFixtureQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserFixtureQueryServiceImpl implements UserFixtureQueryService {

    private final FixtureService fixtureService;
    private final ExternalFootballService externalFootballService;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    private static final Integer CURRENT_SEASON = 2025;

    @Override
    public PageResult<Fixture> page(int page,
                                    int size,
                                    Long leagueId,
                                    Integer season) {

        // 1 current season → MySQL
        if (season == null || season.equals(CURRENT_SEASON)) {

            return fixtureService.page(page, size, leagueId, season);
        }

        String key = "fixtures:" + leagueId + ":" + season + ":" + page + ":" + size;

        try {

            // 2 Redis read
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

            // 3 call API
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

                    // fixture id
                    f.setId(fixtureNode.get("id").asLong());

                    // league + season
                    f.setLeagueId(leagueId);
                    f.setSeason(season);

                    // round
                    if (leagueNode != null && leagueNode.get("round") != null) {
                        f.setRound(leagueNode.get("round").asText());
                    }

                    // teams
                    f.setHomeTeamId(
                            teamsNode.get("home").get("id").asLong()
                    );

                    f.setAwayTeamId(
                            teamsNode.get("away").get("id").asLong()
                    );

                    // goals
                    if (goalsNode != null) {

                        if (!goalsNode.get("home").isNull()) {
                            f.setHomeScore(goalsNode.get("home").asInt());
                        }

                        if (!goalsNode.get("away").isNull()) {
                            f.setAwayScore(goalsNode.get("away").asInt());
                        }
                    }

                    // match time
                    if (fixtureNode.get("date") != null) {

                        String date = fixtureNode.get("date").asText();

                        try {
                            f.setMatchTime(
                                    java.time.OffsetDateTime
                                            .parse(date)
                                            .toLocalDateTime()
                            );
                        } catch (Exception ignored) {}
                    }

                    // status
                    if (fixtureNode.get("status") != null) {

                        JsonNode statusNode = fixtureNode.get("status");

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

            // 4 pagination
            int start = (page - 1) * size;
            int end = Math.min(start + size, list.size());

            List<Fixture> pageList = list.subList(start, end);

            PageResult<Fixture> result =
                    new PageResult<>(list.size(), page, size, pageList);

            // 5 save redis
            stringRedisTemplate.opsForValue()
                    .set(key,
                            objectMapper.writeValueAsString(result),
                            Duration.ofHours(12));

            return result;

        } catch (Exception e) {

            throw new RuntimeException("Historical fixture query failed", e);
        }
    }
}