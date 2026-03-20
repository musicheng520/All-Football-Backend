package com.msc.service.query.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msc.config.FootballProperties;
import com.msc.mapper.FixtureMapper;
import com.msc.model.entity.Fixture;
import com.msc.model.entity.Team;
import com.msc.model.vo.fixture.EventVO;
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
    private final FixtureMapper fixtureMapper;

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

            // =========================
            // 0️⃣ LIVE 优先（Redis）
            // =========================
            String liveJson = stringRedisTemplate.opsForValue().get("live:fixtures");

            if (liveJson != null) {

                JsonNode liveArray = objectMapper.readTree(liveJson);

                for (JsonNode match : liveArray) {

                    long id = match.get("fixture").get("id").asLong();

                    if (id == fixtureId) {

                        System.out.println("[LiveHit] fixture=" + fixtureId);

                        return convertLiveToVO(match);
                    }
                }
            }


            // =========================
            // 1️⃣ Redis（detail缓存）
            // =========================
            String cache = stringRedisTemplate.opsForValue().get(key);

            if (cache != null) {

                System.out.println("[RedisHit] " + key);

                return objectMapper.readValue(cache, FixtureDetailVO.class);
            }

            System.out.println("[RedisMiss] " + key);


            // =========================
            // 2️⃣ DB / 历史
            // =========================
            Fixture fixture = fixtureService.findById(fixtureId);

            FixtureDetailVO vo;

            if (fixture != null
                    && fixture.getSeason() != null
                    && fixture.getSeason().equals(footballProperties.getDefaultSeason())) {

                // 当前赛季
                vo = externalFootballService.buildFixtureDetailFromDb(fixtureId);

            } else {

                // 历史赛季
                vo = externalFootballService.fetchHistoricalFixtureDetail(fixtureId);
            }


            // =========================
            // 3️⃣ 写缓存
            // =========================
            if (vo != null) {

                stringRedisTemplate.opsForValue().set(
                        key,
                        objectMapper.writeValueAsString(vo),
                        Duration.ofHours(12)
                );
            }

            return vo;

        } catch (Exception e) {
            throw new RuntimeException("Fixture detail query failed", e);
        }
    }

    @Override
    public List<JsonNode> getLiveMatches() {

        String cache = stringRedisTemplate.opsForValue().get("live:fixtures");

        // =========================
        // REDIS HIT
        // =========================
        if (cache != null && !cache.isEmpty()) {

            System.out.println("[RedisHit] live matches");

            try {

                return objectMapper.readValue(
                        cache,
                        new TypeReference<List<JsonNode>>() {}
                );

            } catch (Exception e) {

                throw new RuntimeException("Redis parse live matches failed", e);
            }
        }

        // =========================
        // REDIS MISS
        // =========================
        System.out.println("[RedisMiss] live matches");

        return Collections.emptyList();
    }

    public List<Fixture> getRecentMatches() {
        return fixtureMapper.findRecentMatches();
    }

    public List<Fixture> getUpcomingMatches() {
        return fixtureMapper.findUpcomingMatches();
    }

    private FixtureDetailVO convertLiveToVO(JsonNode match) {

        FixtureDetailVO vo = new FixtureDetailVO();

        // =========================
        // fixture
        // =========================
        Fixture fixture = new Fixture();

        JsonNode f = match.get("fixture");

        fixture.setId(f.get("id").asLong());

        fixture.setReferee(f.path("referee").asText(null));
        fixture.setVenue(f.path("venue").path("name").asText(null));

        fixture.setStatus(
                f.path("status").path("short").asText()
        );

        fixture.setHomeScore(match.path("goals").path("home").asInt());
        fixture.setAwayScore(match.path("goals").path("away").asInt());

        vo.setFixture(fixture);

        // =========================
        // teams
        // =========================
        Team home = new Team();
        Team away = new Team();

        JsonNode homeNode = match.path("teams").path("home");
        JsonNode awayNode = match.path("teams").path("away");

        home.setId(homeNode.path("id").asLong());
        home.setName(homeNode.path("name").asText());
        home.setLogo(homeNode.path("logo").asText());

        away.setId(awayNode.path("id").asLong());
        away.setName(awayNode.path("name").asText());
        away.setLogo(awayNode.path("logo").asText());

        vo.setHomeTeam(home);
        vo.setAwayTeam(away);

        // =========================
        // events
        // =========================
        List<EventVO> events = new ArrayList<>();

        for (JsonNode e : match.path("events")) {

            EventVO ev = new EventVO();

            ev.setMinute(e.path("time").path("elapsed").asInt());

            ev.setType(e.path("type").asText());

            ev.setPlayerName(
                    e.path("player").path("name").asText(null)
            );

            ev.setAssistPlayerName(
                    e.path("assist").path("name").asText(null)
            );

            ev.setTeamId(
                    e.path("team").path("id").asLong()
            );

            events.add(ev);
        }

        vo.setEvents(events);

        // =========================
        // stats（live一般没有）
        // =========================
        vo.setStatistics(null);

        // =========================
        // lineup（live没有）
        // =========================
        vo.setHomeLineup(null);
        vo.setAwayLineup(null);

        return vo;
    }
}