package com.msc.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.msc.config.FootballProperties;
import com.msc.mapper.*;
import com.msc.model.entity.*;
import com.msc.model.vo.PlayerDetailVO;
import com.msc.model.vo.PlayerVO;
import com.msc.model.vo.TeamDetailVO;
import com.msc.model.vo.fixture.EventVO;
import com.msc.model.vo.fixture.FixtureDetailVO;
import com.msc.model.vo.fixture.LineupVO;
import com.msc.model.vo.fixture.StatisticVO;
import com.msc.realtime.delta.DeltaManager;
import com.msc.result.PageResult;
import com.msc.service.ExternalFootballService;
import com.msc.service.LivePushService;
import com.msc.service.MatchFinalizeService;
import com.msc.service.PlayerProfileService;
import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ExternalFootballServiceImpl implements ExternalFootballService {

    private final RestTemplate restTemplate;
    private final FootballProperties properties;
    private final ObjectMapper objectMapper;
    private final TeamMapper teamMapper;
    private final FixtureMapper fixtureMapper;
    private final PlayerMapper playerMapper;
    private final PlayerStatsMapper playerStatsMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final DeltaManager deltaManager;
    private final LivePushService livePushService;
    @Lazy
    private final MatchFinalizeService matchFinalizeService;

    private final MatchEventMapper matchEventMapper;
    private final LineupPlayerMapper lineupPlayerMapper;
    private final LineupMapper lineupMapper;
    private final MatchStatisticMapper matchStatisticMapper;
    private final PlayerProfileService playerProfileService;
    private final SimpMessagingTemplate simpMessagingTemplate;

    @Override
    public String fetchTeams(Long leagueId, Integer season) {

        String url = "https://" + properties.getApi().getHost()
                + "/teams?league=" + leagueId + "&season=" + season;

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-apisports-key", properties.getApi().getKey());

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response =
                restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        return response.getBody();
    }

    @Override
    public PageResult<Team> fetchTeamsForQuery(Long leagueId, Integer season, int page, int size) {

        String json = fetchTeams(leagueId, season);

        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode response = root.get("response");

            List<Team> teams = new ArrayList<>();

            if (response != null && response.isArray()) {
                for (JsonNode item : response) {

                    JsonNode teamNode = item.get("team");
                    JsonNode venueNode = item.get("venue");

                    if (teamNode == null || teamNode.get("id") == null) {
                        continue;
                    }

                    Team team = new Team();

                    team.setId(teamNode.get("id").asLong());
                    team.setName(textOrNull(teamNode.get("name")));
                    team.setCode(textOrNull(teamNode.get("code")));
                    team.setCountry(textOrNull(teamNode.get("country")));
                    team.setFounded(intOrNull(teamNode.get("founded")));
                    team.setLogo(textOrNull(teamNode.get("logo")));

                    if (venueNode != null && !venueNode.isNull()) {
                        team.setVenueId(longOrNull(venueNode.get("id")));
                        team.setVenueName(textOrNull(venueNode.get("name")));
                        team.setVenueCity(textOrNull(venueNode.get("city")));
                        team.setVenueCapacity(intOrNull(venueNode.get("capacity")));
                        team.setVenueSurface(textOrNull(venueNode.get("surface")));
                        team.setVenueImage(textOrNull(venueNode.get("image")));
                    }

                    team.setLeagueId(leagueId);
                    team.setSeason(season);

                    teams.add(team);
                }
            }

            int start = (page - 1) * size;
            if (start >= teams.size()) {
                return new PageResult<>(teams.size(), page, size, List.of());
            }

            int end = Math.min(start + size, teams.size());
            List<Team> pageList = teams.subList(start, end);

            return new PageResult<>(teams.size(), page, size, pageList);

        } catch (Exception e) {
            throw new RuntimeException("Historical team query failed", e);
        }
    }

    @Override
    @Transactional
    public void syncPlayerStats(Integer season) {

        List<Long> leagues = properties.getSupportedLeagues();

        for (Long leagueId : leagues) {

            int page = 1;
            int totalPages;

            do {

                String url = "https://" + properties.getApi().getHost()
                        + "/players?league=" + leagueId
                        + "&season=" + season
                        + "&page=" + page;

                HttpHeaders headers = new HttpHeaders();
                headers.set("x-apisports-key", properties.getApi().getKey());

                HttpEntity<String> entity = new HttpEntity<>(headers);

                ResponseEntity<String> response =
                        restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

                try {

                    JsonNode root = objectMapper.readTree(response.getBody());

                    totalPages = root.get("paging").get("total").asInt();
                    JsonNode responseArr = root.get("response");

                    int count = 0;

                    for (JsonNode item : responseArr) {

                        JsonNode playerNode = item.get("player");
                        JsonNode statsArr = item.get("statistics");

                        if (statsArr == null || statsArr.size() == 0)
                            continue;

                        Long playerId = playerNode.get("id").asLong();

                        // ✅ 关键：确保 players 表里存在
                        if (playerMapper.findById(playerId) == null) {
                            continue;
                        }

                        JsonNode stat = statsArr.get(0);

                        PlayerStats ps = new PlayerStats();

                        ps.setPlayerId(playerId);
                        ps.setLeagueId(leagueId);
                        ps.setSeason(season);

                        if (stat.has("team") && !stat.get("team").isNull()) {
                            ps.setTeamId(
                                    stat.get("team").get("id").asLong()
                            );
                        }

                        // ⚠ API 拼写是 appearences
                        ps.setAppearances(
                                intOrNull(stat.get("games").get("appearences"))
                        );

                        ps.setGoals(
                                intOrNull(stat.get("goals").get("total"))
                        );

                        ps.setAssists(
                                intOrNull(stat.get("goals").get("assists"))
                        );

                        ps.setYellowCards(
                                intOrNull(stat.get("cards").get("yellow"))
                        );

                        ps.setRedCards(
                                intOrNull(stat.get("cards").get("red"))
                        );

                        ps.setRating(
                                textOrNull(stat.get("games").get("rating"))
                        );

                        playerStatsMapper.upsert(ps);
                        count++;
                    }

                    System.out.println("[PlayerStatsSync] league=" + leagueId
                            + " page=" + page
                            + "/" + totalPages
                            + " upserted=" + count);

                    page++;

                } catch (Exception e) {
                    e.printStackTrace();
                    break;
                }

            } while (page <= totalPages);
        }

        System.out.println("[PlayerStatsSync] Completed season=" + season);
    }

    @Override
    @Transactional
    public void syncMatchEvents(Integer season) {

        List<Fixture> fixtures = fixtureMapper.findBySeason(season);

        int total = 0;

        for (Fixture fixture : fixtures) {

            try {

                // ✅ 改这里（关键）
                String json = fetchFixtureEvents(fixture.getId());

                JsonNode root = objectMapper.readTree(json);
                JsonNode events = root.get("response");

                if (events == null || !events.isArray() || events.isEmpty()) {
                    continue;
                }

                List<MatchEvent> list = new ArrayList<>();

                for (JsonNode e : events) {

                    MatchEvent event = new MatchEvent();

                    event.setFixtureId(fixture.getId());
                    event.setTeamId(longOrNull(e.path("team").path("id")));
                    event.setPlayerId(longOrNull(e.path("player").path("id")));
                    event.setAssistPlayerId(longOrNull(e.path("assist").path("id")));

                    event.setType(textOrNull(e.get("type")));
                    event.setDetail(textOrNull(e.get("detail")));

                    event.setMinute(intOrNull(e.path("time").path("elapsed")));
                    event.setExtraMinute(intOrNull(e.path("time").path("extra")));

                    event.setCreatedAt(LocalDateTime.now());

                    list.add(event);
                }

                if (!list.isEmpty()) {

                    // ✅ 幂等（建议）
                    matchEventMapper.deleteByFixtureId(fixture.getId());

                    matchEventMapper.insertBatch(list);

                    total += list.size();
                }

                // ✅ 限流（必须）
                Thread.sleep(200);

            } catch (Exception e) {
                System.out.println("[WARN] event fail fixture=" + fixture.getId());
            }
        }

        System.out.println("[MatchEventsSync] total=" + total);
    }

    @Override
    @Transactional
    public void syncLineups(Integer season) {

        List<Fixture> fixtures = fixtureMapper.findBySeason(season);

        int total = 0;

        for (Fixture fixture : fixtures) {

            try {

                // ✅ 改这里（关键）
                String json = fetchFixtureLineups(fixture.getId());

                JsonNode root = objectMapper.readTree(json);
                JsonNode lineups = root.get("response");

                if (lineups == null || !lineups.isArray() || lineups.isEmpty()) {
                    continue;
                }

                for (JsonNode l : lineups) {

                    Long teamId = longOrNull(l.path("team").path("id"));
                    String formation = textOrNull(l.get("formation"));
                    String coach = textOrNull(l.path("coach").path("name"));

                    Lineup lineup = new Lineup();

                    lineup.setFixtureId(fixture.getId());
                    lineup.setTeamId(teamId);
                    lineup.setFormation(formation);
                    lineup.setCoach(coach);
                    lineup.setCreatedAt(LocalDateTime.now());

                    lineupMapper.insert(lineup);

                    Long lineupId = lineup.getId();

                    List<LineupPlayer> players = new ArrayList<>();

                    // start XI
                    for (JsonNode p : l.path("startXI")) {

                        LineupPlayer lp = new LineupPlayer();

                        lp.setLineupId(lineupId);
                        lp.setPlayerId(longOrNull(p.path("player").path("id")));
                        lp.setNumber(intOrNull(p.path("player").path("number")));
                        lp.setPosition(textOrNull(p.path("player").path("pos")));

                        lp.setIsStarting(1);
                        lp.setCreatedAt(LocalDateTime.now());

                        players.add(lp);
                    }

                    // subs
                    for (JsonNode p : l.path("substitutes")) {

                        LineupPlayer lp = new LineupPlayer();

                        lp.setLineupId(lineupId);
                        lp.setPlayerId(longOrNull(p.path("player").path("id")));
                        lp.setNumber(intOrNull(p.path("player").path("number")));
                        lp.setPosition(textOrNull(p.path("player").path("pos")));

                        lp.setIsStarting(0);
                        lp.setCreatedAt(LocalDateTime.now());

                        players.add(lp);
                    }

                    if (!players.isEmpty()) {
                        lineupPlayerMapper.insertBatch(players);
                        total += players.size();
                    }
                }

                // ✅ 限流
                Thread.sleep(200);

            } catch (Exception e) {
                System.out.println("[WARN] lineup fail fixture=" + fixture.getId());
            }
        }

        System.out.println("[LineupSync] total players=" + total);
    }

    @Override
    @Transactional
    public void syncYesterdayMatches() {

        LocalDate yesterday = LocalDate.now().minusDays(1);

        List<Fixture> fixtures = fixtureMapper.findByDate(yesterday);

        if (fixtures == null || fixtures.isEmpty()) {
            System.out.println("[YesterdaySync] no fixtures found");
            return;
        }

        int count = 0;

        for (Fixture fixture : fixtures) {

            try {

                String status = fixture.getStatus();

                if ("FT".equals(status) || "AET".equals(status) || "PEN".equals(status)) {

                    matchFinalizeService.finalizeMatch(fixture.getId());
                    count++;
                }

            } catch (Exception e) {

                System.out.println("[YesterdaySync] fail fixture=" + fixture.getId());
                e.printStackTrace();
            }
        }

        System.out.println("[YesterdaySync] finalized=" + count);
    }

    @Override
    @Transactional
    public void weeklyBaseSync(Integer season) {

        System.out.println("[WeeklyBaseSync] start season=" + season);

        syncTeams(season);
        syncPlayers(season);
        syncAllFixtures(season);
        syncPlayerStats(season);

        System.out.println("[WeeklyBaseSync] completed season=" + season);
    }


    @Override
    @Transactional
    public void syncMatchStatistics(Integer season) {

        List<Fixture> fixtures = fixtureMapper.findBySeason(season);

        int total = 0;
        int successFixtures = 0;
        int failFixtures = 0;

        for (Fixture fixture : fixtures) {

            try {

                // ✅ 改这里（关键）
                String json = fetchFixtureStatistics(fixture.getId());

                JsonNode root = objectMapper.readTree(json);
                JsonNode statistics = root.get("response");

                if (statistics == null || !statistics.isArray() || statistics.isEmpty()) {
                    continue;
                }

                List<MatchStatistic> list = new ArrayList<>();

                for (JsonNode team : statistics) {

                    Long teamId = longOrNull(team.path("team").path("id"));

                    MatchStatistic stat = new MatchStatistic();

                    stat.setFixtureId(fixture.getId());
                    stat.setTeamId(teamId);

                    for (JsonNode s : team.path("statistics")) {

                        String type = textOrNull(s.get("type"));
                        String value = textOrNull(s.get("value"));

                        if (type == null) continue;

                        switch (type) {

                            case "Total Shots":
                                stat.setShotsTotal(intOrNull(s.get("value")));
                                break;
                            case "Shots on Goal":
                                stat.setShotsOnTarget(intOrNull(s.get("value")));
                                break;
                            case "Ball Possession":
                                stat.setPossession(value);
                                break;
                            case "Fouls":
                                stat.setFouls(intOrNull(s.get("value")));
                                break;
                            case "Corner Kicks":
                                stat.setCorners(intOrNull(s.get("value")));
                                break;
                            case "Yellow Cards":
                                stat.setYellowCards(intOrNull(s.get("value")));
                                break;
                            case "Red Cards":
                                stat.setRedCards(intOrNull(s.get("value")));
                                break;
                            case "Offsides":
                                stat.setOffsides(intOrNull(s.get("value")));
                                break;
                        }
                    }

                    stat.setCreatedAt(LocalDateTime.now());
                    list.add(stat);
                }

                if (!list.isEmpty()) {

                    matchStatisticMapper.deleteByFixtureId(fixture.getId());
                    matchStatisticMapper.insertBatch(list);

                    total += list.size();
                    successFixtures++;
                }

                // ✅ 限流
                Thread.sleep(200);

            } catch (Exception e) {
                failFixtures++;
                System.out.println("[WARN] stat fail fixture=" + fixture.getId());
            }
        }

        System.out.println(
                "[MatchStatisticsSync] total=" + total +
                        ", successFixtures=" + successFixtures +
                        ", failFixtures=" + failFixtures
        );
    }

    @Override
    @Transactional
    public void syncTeams(Integer season) {

        List<Long> leagues = properties.getSupportedLeagues();

        for (Long leagueId : leagues) {

            String json = fetchTeams(leagueId, season);

            try {
                JsonNode root = objectMapper.readTree(json);
                JsonNode responseArr = root.get("response");

                if (responseArr == null || !responseArr.isArray()) {
                    System.out.println("[TeamsSync] league=" + leagueId + " season=" + season
                            + " response is empty or invalid");
                    continue;
                }

                int count = 0;

                for (JsonNode item : responseArr) {

                    JsonNode teamNode = item.get("team");
                    JsonNode venueNode = item.get("venue");

                    if (teamNode == null || teamNode.get("id") == null) {
                        continue;
                    }

                    Team team = new Team();

                    // Use API team.id as PK (matches your DB design)
                    team.setId(teamNode.get("id").asLong());

                    team.setName(textOrNull(teamNode.get("name")));
                    team.setCode(textOrNull(teamNode.get("code")));
                    team.setCountry(textOrNull(teamNode.get("country")));
                    team.setFounded(intOrNull(teamNode.get("founded")));
                    team.setIsNational(
                            teamNode.get("national") != null
                                    && teamNode.get("national").asBoolean(false)
                    );
                    team.setLogo(textOrNull(teamNode.get("logo")));

                    if (venueNode != null && !venueNode.isNull()) {
                        team.setVenueId(longOrNull(venueNode.get("id")));
                        team.setVenueName(textOrNull(venueNode.get("name")));
                        team.setVenueCity(textOrNull(venueNode.get("city")));
                        team.setVenueCapacity(intOrNull(venueNode.get("capacity")));
                        team.setVenueSurface(textOrNull(venueNode.get("surface")));
                        team.setVenueImage(textOrNull(venueNode.get("image")));
                    }

                    team.setLeagueId(leagueId);
                    team.setSeason(season);

                    teamMapper.upsert(team);
                    count++;
                }

                System.out.println("[TeamsSync] league=" + leagueId + " season=" + season
                        + " upserted=" + count);

            } catch (Exception e) {
                // Do not crash the whole sync if one league fails parsing
                System.out.println("[TeamsSync] league=" + leagueId + " season=" + season
                        + " parse failed: " + e.getMessage());
            }
        }
    }




    @Override
    @Transactional
    public void syncFixtures(Integer season, Long leagueId) {

        String url = "https://" + properties.getApi().getHost()
                + "/fixtures?league=" + leagueId
                + "&season=" + season;

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-apisports-key", properties.getApi().getKey());

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response =
                restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        try {

            JsonNode root = objectMapper.readTree(response.getBody());

            JsonNode responseArr = root.get("response");

            if (responseArr == null || responseArr.size() == 0) {
                System.out.println("[FixtureSync] league=" + leagueId + " empty.");
                return;
            }

            int count = 0;

            for (JsonNode item : responseArr) {

                JsonNode fixtureNode = item.get("fixture");
                JsonNode leagueNode = item.get("league");
                JsonNode teamsNode = item.get("teams");
                JsonNode goalsNode = item.get("goals");

                Fixture fixture = new Fixture();

                fixture.setId(fixtureNode.get("id").asLong());
                fixture.setLeagueId(leagueNode.get("id").asLong());
                fixture.setSeason(leagueNode.get("season").asInt());
                fixture.setRound(textOrNull(leagueNode.get("round")));

                fixture.setHomeTeamId(
                        teamsNode.get("home").get("id").asLong());
                fixture.setAwayTeamId(
                        teamsNode.get("away").get("id").asLong());

                fixture.setHomeScore(intOrNull(goalsNode.get("home")));
                fixture.setAwayScore(intOrNull(goalsNode.get("away")));

                fixture.setStatus(
                        fixtureNode.get("status").get("short").asText());

                fixture.setElapsed(
                        intOrNull(fixtureNode.get("status").get("elapsed")));

                fixture.setReferee(textOrNull(fixtureNode.get("referee")));
                fixture.setVenue(
                        textOrNull(fixtureNode.get("venue").get("name")));

                fixture.setMatchTime(
                        OffsetDateTime.parse(
                                fixtureNode.get("date").asText()
                        ).toLocalDateTime()
                );

                fixtureMapper.upsert(fixture);
                count++;
            }

            System.out.println("[FixtureSync] league=" + leagueId
                    + " season=" + season
                    + " upserted=" + count);

        } catch (Exception e) {
            throw new RuntimeException("Fixture sync failed", e);
        }
    }

    @Override
    @Transactional
    public void syncAllFixtures(Integer season) {

        List<Long> leagues = properties.getSupportedLeagues();

        if (leagues == null || leagues.isEmpty()) {
            System.out.println("[FixtureSync] No supported leagues configured.");
            return;
        }

        for (Long leagueId : leagues) {
            try {
                syncFixtures(season, leagueId);
            } catch (Exception e) {
                System.out.println("[FixtureSync] Failed for league=" + leagueId);
                e.printStackTrace();
            }
        }

        System.out.println("[FixtureSync] All leagues completed for season=" + season);
    }

    @Override
    @Transactional
    public void syncPlayers(Integer season) {

        List<Team> teams = teamMapper.findBySeason(season);

        if (teams == null || teams.isEmpty()) {
            System.out.println("[PlayerSync] No teams found.");
            return;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-apisports-key", properties.getApi().getKey());

        HttpEntity<String> entity = new HttpEntity<>(headers);

        for (Team team : teams) {

            System.out.println("\n========== Team " + team.getId() + " ==========");

            String url = "https://" + properties.getApi().getHost()
                    + "/players/squads?team=" + team.getId();

            try {

                ResponseEntity<String> response =
                        restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode responseArr = root.get("response");

                if (responseArr == null || responseArr.size() == 0) {
                    System.out.println("[Squad] Empty for team=" + team.getId());
                    continue;
                }

                JsonNode playersArr = responseArr.get(0).get("players");

                int count = 0;

                for (JsonNode p : playersArr) {

                    Player player = new Player();

                    // ================================
                    // 基础信息
                    // ================================
                    player.setId(p.get("id").asLong());
                    player.setName(textOrNull(p.get("name")));
                    player.setFirstName(textOrNull(p.get("firstname")));
                    player.setLastName(textOrNull(p.get("lastname")));
                    player.setAge(intOrNull(p.get("age")));

                    player.setPhoto(textOrNull(p.get("photo")));
                    player.setNationality(textOrNull(p.get("nationality")));

                    // ================================
                    // birth 信息（重点：防崩）
                    // ================================
                    if (p.has("birth") && !p.get("birth").isNull()) {

                        JsonNode birth = p.get("birth");

                        // date
                        if (birth.has("date") && !birth.get("date").isNull()) {
                            try {
                                player.setBirthDate(
                                        LocalDate.parse(birth.get("date").asText())
                                );
                            } catch (Exception e) {
                                System.out.println(
                                        "[Birth Parse Error] player=" + player.getId()
                                );
                            }
                        }

                        // place
                        if (birth.has("place")) {
                            player.setBirthPlace(textOrNull(birth.get("place")));
                        }

                        // country
                        if (birth.has("country")) {
                            player.setBirthCountry(textOrNull(birth.get("country")));
                        }
                    }

                    // ================================
                    // 身体信息（可能大量为 null）
                    // ================================
                    player.setHeight(
                            p.has("height") && !p.get("height").isNull()
                                    ? p.get("height").asText()
                                    : null
                    );

                    player.setWeight(
                            p.has("weight") && !p.get("weight").isNull()
                                    ? p.get("weight").asText()
                                    : null
                    );

                    // ================================
                    // 关联信息
                    // ================================
                    player.setTeamId(team.getId());
                    player.setLeagueId(team.getLeagueId());
                    player.setSeason(season);

                    // ================================
                    // 数据库写入
                    // ================================
                    playerMapper.upsert(player);

                    count++;

                    // ================================
                    // Debug 输出（你要求的 println）
                    // ================================
                    System.out.println(
                            "[Player] id=" + player.getId()
                                    + " | name=" + player.getName()
                                    + " | nat=" + player.getNationality()
                                    + " | height=" + player.getHeight()
                                    + " | weight=" + player.getWeight()
                                    + " | birth=" + player.getBirthDate()
                    );
                }

                System.out.println("[Squad] saved=" + count);

            } catch (Exception e) {
                System.out.println("[ERROR] team=" + team.getId());
                e.printStackTrace();
            }
        }

        System.out.println("\n[PlayerSync] Completed season=" + season);
    }

    @Override
    public String fetchLiveFixturesFilteredJson() {

        String url = "https://" + properties.getApi().getHost()
                + "/fixtures?live=all";

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-apisports-key", properties.getApi().getKey());

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response =
                restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        try {

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode responseArr = root.get("response");

            int total = (responseArr == null) ? 0 : responseArr.size();

            List<Long> allowed = properties.getSupportedLeagues();
            ArrayNode filtered = objectMapper.createArrayNode();

            if (responseArr != null) {
                for (JsonNode item : responseArr) {

                    JsonNode leagueNode = item.get("league");

                    if (leagueNode == null || leagueNode.get("id") == null) {
                        continue;
                    }

                    long leagueId = leagueNode.get("id").asLong();

                    if (allowed != null && allowed.contains(leagueId)) {
                        filtered.add(item);
                    }
                }
            }

            System.out.println("[LiveFetch] total=" + total
                    + " filtered=" + filtered.size());

            return filtered.toString();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String fetchFixtureById(Long fixtureId) {

        String url = "https://" + properties.getApi().getHost()
                + "/fixtures?id=" + fixtureId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-apisports-key", properties.getApi().getKey());

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response =
                restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        return response.getBody();
    }

    @Override
    public String fetchFixturesBySeason(Long leagueId, Integer season) {

        String url = "https://" + properties.getApi().getHost()
                + "/fixtures?league=" + leagueId
                + "&season=" + season;

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-apisports-key", properties.getApi().getKey());

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response =
                restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        return response.getBody();
    }




    @Override
    public void refreshLiveSnapshotToRedis() {

        try {

            // 1️. read old snapshot
            String oldJson = stringRedisTemplate.opsForValue()
                    .get("live:fixtures");

            JsonNode oldSnapshot = null;
            if (oldJson != null && !oldJson.isEmpty()) {
                oldSnapshot = objectMapper.readTree(oldJson);
            }

            // 2️. get new snapshot
            String newJson = fetchLiveFixturesFilteredJson();
            JsonNode newSnapshot = objectMapper.readTree(newJson);

            // 3️. cold start
            if (oldSnapshot == null) {

                stringRedisTemplate.opsForValue()
                        .set("live:fixtures",
                                newJson,
                                Duration.ofSeconds(60));

                System.out.println("[LiveSnapshot] initialized (cold start)");
                return;
            }

            // 4️. old map (O(n))
            Map<Long, JsonNode> oldMatchMap = new HashMap<>();

            for (JsonNode match : oldSnapshot) {

                long id = match.get("fixture")
                        .get("id")
                        .asLong();

                oldMatchMap.put(id, match);
            }

            // =========================
            // 5️. delta detection (正常LIVE变化)
            // =========================
            for (JsonNode newMatch : newSnapshot) {

                long fixtureId = newMatch.get("fixture")
                        .get("id")
                        .asLong();

                JsonNode oldMatch = oldMatchMap.get(fixtureId);

                List<String> changes =
                        deltaManager.detectChanges(oldMatch, newMatch);

                if (!changes.isEmpty()) {

                    System.out.println(
                            "Match " + fixtureId +
                                    " changed → " + changes
                    );

                    //  推送 LIVE 更新
                    simpMessagingTemplate.convertAndSend("/topic/live", newMatch);

                    simpMessagingTemplate.convertAndSend(
                            "/topic/match/" + fixtureId,
                            newMatch
                    );

                    // -------- 正常 FT（极少出现，但保留）--------
                    String status = newMatch
                            .get("fixture")
                            .get("status")
                            .get("short")
                            .asText();

                    if ("FT".equals(status) || "AET".equals(status) || "PEN".equals(status)) {

                        Long added = stringRedisTemplate.opsForSet()
                                .add("finished:fixtures", String.valueOf(fixtureId));

                        if (added != null && added == 1L) {

                            System.out.println(
                                    "[FinalizeTrigger] fixture=" + fixtureId + " status=" + status
                            );

                            matchFinalizeService.finalizeMatch(fixtureId);
                        }
                    }
                }
            }

            // =========================
            // 6️.  核心：检测“消失的比赛”（FT）
            // =========================

            Set<Long> newIds = new HashSet<>();

            for (JsonNode m : newSnapshot) {
                long id = m.get("fixture").get("id").asLong();
                newIds.add(id);
            }

            for (Long oldId : oldMatchMap.keySet()) {

                //  old有，new没有 → 比赛结束
                if (!newIds.contains(oldId)) {

                    System.out.println("[FT detected] fixture=" + oldId);

                    //  构造 FT 推送
                    Map<String, Object> ftMsg = Map.of(
                            "fixture", Map.of(
                                    "id", oldId,
                                    "status", Map.of("short", "FT")
                            )
                    );

                    simpMessagingTemplate.convertAndSend("/topic/live", ftMsg);

                    simpMessagingTemplate.convertAndSend(
                            "/topic/match/" + oldId,
                            ftMsg
                    );

                    //  finalize（只执行一次）
                    Long added = stringRedisTemplate.opsForSet()
                            .add("finished:fixtures", String.valueOf(oldId));

                    if (added != null && added == 1L) {

                        System.out.println("[FinalizeTrigger] FT detected " + oldId);

                        matchFinalizeService.finalizeMatch(oldId);
                    }
                }
            }

            // =========================
            // 7️. update snapshot
            // =========================
            stringRedisTemplate.opsForValue()
                    .set("live:fixtures",
                            newJson,
                            Duration.ofSeconds(60));

            System.out.println("[LiveSnapshot] updated");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public FixtureDetailVO buildFixtureDetailFromDb(Long fixtureId) {

        Fixture fixture = fixtureMapper.findById(fixtureId);

        if (fixture == null) {
            return null;
        }

        FixtureDetailVO vo = new FixtureDetailVO();
        vo.setFixture(fixture);

        // teams
        Team homeTeam = teamMapper.findById(fixture.getHomeTeamId());
        Team awayTeam = teamMapper.findById(fixture.getAwayTeamId());

        vo.setHomeTeam(homeTeam);
        vo.setAwayTeam(awayTeam);

        // ---------- lineups ----------
        List<Lineup> lineups = lineupMapper.findByFixtureId(fixtureId);
        List<LineupPlayer> lineupPlayers = lineupPlayerMapper.findByFixtureId(fixtureId);

        for (Lineup l : lineups) {

            LineupVO lineupVO = new LineupVO();
            lineupVO.setTeamId(l.getTeamId());
            lineupVO.setFormation(l.getFormation());
            lineupVO.setCoach(l.getCoach());

            List<LineupPlayer> starting = new ArrayList<>();
            List<LineupPlayer> subs = new ArrayList<>();

            for (LineupPlayer p : lineupPlayers) {
                if (!p.getLineupId().equals(l.getId())) continue;

                if (p.getIsStarting() != null && p.getIsStarting() == 1) {
                    starting.add(p);
                } else {
                    subs.add(p);
                }
            }

            lineupVO.setStartingXI(starting);
            lineupVO.setSubstitutes(subs);

            if (l.getTeamId().equals(fixture.getHomeTeamId())) {
                vo.setHomeLineup(lineupVO);
            } else {
                vo.setAwayLineup(lineupVO);
            }
        }

        // ---------- events ----------
        List<MatchEvent> events = matchEventMapper.findByFixtureId(fixtureId);
        List<EventVO> eventVOList = new ArrayList<>();

        for (MatchEvent e : events) {

            EventVO ev = new EventVO();

            ev.setMinute(e.getMinute());
            ev.setTeamId(e.getTeamId());

            ev.setPlayerId(e.getPlayerId());
            ev.setAssistPlayerId(e.getAssistPlayerId());

            if (e.getPlayerId() != null) {
                Player p = playerMapper.findById(e.getPlayerId());
                if (p != null) ev.setPlayerName(p.getName());
            }

            if (e.getAssistPlayerId() != null) {
                Player p = playerMapper.findById(e.getAssistPlayerId());
                if (p != null) ev.setAssistPlayerName(p.getName());
            }

            ev.setType(e.getType());
            ev.setDetail(e.getDetail());

            eventVOList.add(ev);
        }

        vo.setEvents(eventVOList);

        // ---------- statistics ----------
        List<MatchStatistic> stats = matchStatisticMapper.findByFixtureId(fixtureId);

        StatisticVO statisticVO = new StatisticVO();

        for (MatchStatistic s : stats) {
            if (s.getTeamId().equals(fixture.getHomeTeamId())) {
                statisticVO.setHome(s);
            } else if (s.getTeamId().equals(fixture.getAwayTeamId())) {
                statisticVO.setAway(s);
            }
        }

        vo.setStatistics(statisticVO);

        return vo;
    }
    @Override
    public FixtureDetailVO fetchHistoricalFixtureDetail(Long fixtureId) {

        try {

            // 1 基础比赛信息
            String fixtureJson = fetchFixtureById(fixtureId);

            JsonNode root = objectMapper.readTree(fixtureJson);
            JsonNode response = root.get("response");

            if (response == null || response.isEmpty()) {
                return null;
            }

            JsonNode item = response.get(0);

            JsonNode fixtureNode = item.get("fixture");
            JsonNode leagueNode = item.get("league");
            JsonNode teamsNode = item.get("teams");
            JsonNode goalsNode = item.get("goals");

            Fixture fixture = new Fixture();

            fixture.setId(fixtureNode.get("id").asLong());

            if (leagueNode != null) {
                fixture.setLeagueId(leagueNode.get("id").asLong());
                fixture.setSeason(leagueNode.get("season").asInt());
                fixture.setRound(leagueNode.get("round").asText());
            }

            if (teamsNode != null) {
                fixture.setHomeTeamId(teamsNode.get("home").get("id").asLong());
                fixture.setAwayTeamId(teamsNode.get("away").get("id").asLong());
            }

            if (goalsNode != null) {
                if (!goalsNode.get("home").isNull()) {
                    fixture.setHomeScore(goalsNode.get("home").asInt());
                }
                if (!goalsNode.get("away").isNull()) {
                    fixture.setAwayScore(goalsNode.get("away").asInt());
                }
            }

            if (fixtureNode.get("date") != null) {
                fixture.setMatchTime(
                        OffsetDateTime.parse(fixtureNode.get("date").asText())
                                .toLocalDateTime()
                );
            }

            if (fixtureNode.get("referee") != null && !fixtureNode.get("referee").isNull()) {
                fixture.setReferee(fixtureNode.get("referee").asText());
            }

            if (fixtureNode.get("venue") != null) {
                JsonNode venue = fixtureNode.get("venue");
                if (venue.get("name") != null && !venue.get("name").isNull()) {
                    fixture.setVenue(venue.get("name").asText());
                }
            }

            JsonNode statusNode = fixtureNode.get("status");

            if (statusNode != null) {
                fixture.setStatus(statusNode.get("short").asText());

                if (!statusNode.get("elapsed").isNull()) {
                    fixture.setElapsed(statusNode.get("elapsed").asInt());
                }
            }

            FixtureDetailVO vo = new FixtureDetailVO();
            vo.setFixture(fixture);

            // 2 teams
            if (teamsNode != null) {

                JsonNode home = teamsNode.get("home");
                JsonNode away = teamsNode.get("away");

                Team homeTeam = new Team();
                homeTeam.setId(home.get("id").asLong());
                homeTeam.setName(home.get("name").asText());
                homeTeam.setLogo(home.get("logo").asText());

                Team awayTeam = new Team();
                awayTeam.setId(away.get("id").asLong());
                awayTeam.setName(away.get("name").asText());
                awayTeam.setLogo(away.get("logo").asText());

                vo.setHomeTeam(homeTeam);
                vo.setAwayTeam(awayTeam);
            }

            // 3 lineups
            String lineupJson = fetchFixtureLineups(fixtureId);

            JsonNode lineupRoot = objectMapper.readTree(lineupJson);
            JsonNode lineupResp = lineupRoot.get("response");

            if (lineupResp != null && lineupResp.isArray()) {

                List<Lineup> lineups = new ArrayList<>();

                for (JsonNode node : lineupResp) {

                    Lineup lineup = new Lineup();

                    lineup.setFixtureId(fixtureId);

                    JsonNode team = node.get("team");

                    if (team != null) {
                        lineup.setTeamId(team.get("id").asLong());
                    }

                    if (node.get("formation") != null) {
                        lineup.setFormation(node.get("formation").asText());
                    }

                    if (node.get("coach") != null && node.get("coach").get("name") != null) {
                        lineup.setCoach(node.get("coach").get("name").asText());
                    }

                    lineups.add(lineup);
                }

                LineupVO homeLineup = null;
                LineupVO awayLineup = null;

                for (JsonNode node : lineupResp) {

                    LineupVO lineup = new LineupVO();

                    JsonNode team = node.get("team");

                    Long teamId = team.get("id").asLong();

                    lineup.setTeamId(teamId);
                    lineup.setFormation(textOrNull(node.get("formation")));

                    if (node.get("coach") != null) {
                        lineup.setCoach(textOrNull(node.get("coach").get("name")));
                    }

                    if (teamId.equals(fixture.getHomeTeamId())) {
                        homeLineup = lineup;
                    } else {
                        awayLineup = lineup;
                    }
                }

                vo.setHomeLineup(homeLineup);
                vo.setAwayLineup(awayLineup);
            }

            // 4 events
            String eventsJson = fetchFixtureEvents(fixtureId);

            JsonNode eventRoot = objectMapper.readTree(eventsJson);
            JsonNode eventResp = eventRoot.get("response");

            if (eventResp != null && eventResp.isArray()) {

                List<EventVO> events = new ArrayList<>();

                for (JsonNode node : eventResp) {

                    EventVO event = new EventVO();

                    if (node.get("time") != null) {
                        event.setMinute(intOrNull(node.get("time").get("elapsed")));
                    }

                    if (node.get("team") != null) {
                        event.setTeamId(longOrNull(node.get("team").get("id")));
                    }

                    if (node.get("player") != null) {
                        event.setPlayerId(longOrNull(node.get("player").get("id")));
                        event.setPlayerName(textOrNull(node.get("player").get("name")));
                    }

                    if (node.get("assist") != null) {
                        event.setAssistPlayerId(longOrNull(node.get("assist").get("id")));
                        event.setAssistPlayerName(textOrNull(node.get("assist").get("name")));
                    }

                    event.setType(textOrNull(node.get("type")));
                    event.setDetail(textOrNull(node.get("detail")));

                    events.add(event);
                }

                vo.setEvents(events);
            }

            // 5 statistics
            String statJson = fetchFixtureStatistics(fixtureId);

            JsonNode statRoot = objectMapper.readTree(statJson);
            JsonNode statResp = statRoot.get("response");

            if (statResp != null && statResp.isArray()) {

                StatisticVO statisticVO = new StatisticVO();

                for (JsonNode node : statResp) {

                    MatchStatistic stat = new MatchStatistic();

                    stat.setFixtureId(fixtureId);
                    stat.setTeamId(longOrNull(node.get("team").get("id")));

                    JsonNode statistics = node.get("statistics");

                    for (JsonNode s : statistics) {

                        String type = textOrNull(s.get("type"));
                        JsonNode value = s.get("value");

                        if (type == null || value == null || value.isNull()) continue;

                        switch (type) {

                            case "Total Shots":
                                stat.setShotsTotal(intOrNull(value));
                                break;

                            case "Shots on Goal":
                                stat.setShotsOnTarget(intOrNull(value));
                                break;

                            case "Ball Possession":
                                stat.setPossession(value.asText());
                                break;

                            case "Corner Kicks":
                                stat.setCorners(intOrNull(value));
                                break;

                            case "Fouls":
                                stat.setFouls(intOrNull(value));
                                break;

                            case "Yellow Cards":
                                stat.setYellowCards(intOrNull(value));
                                break;

                            case "Red Cards":
                                stat.setRedCards(intOrNull(value));
                                break;

                            case "Offsides":
                                stat.setOffsides(intOrNull(value));
                                break;
                        }
                    }

                    if (stat.getTeamId().equals(fixture.getHomeTeamId())) {
                        statisticVO.setHome(stat);
                    } else {
                        statisticVO.setAway(stat);
                    }
                }

                vo.setStatistics(statisticVO);
            }

            return vo;

        } catch (Exception e) {

            throw new RuntimeException("Historical fixture detail query failed", e);
        }
    }

    @Override
    public TeamDetailVO fetchTeamDetail(Long teamId, Integer season) {

        try {

            HttpHeaders headers = new HttpHeaders();
            headers.set("x-apisports-key", properties.getApi().getKey());

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // ---------- players ----------

            String playersUrl =
                    "https://v3.football.api-sports.io/players?team="
                            + teamId + "&season=" + season;

            ResponseEntity<String> playersResp =
                    restTemplate.exchange(playersUrl, HttpMethod.GET, entity, String.class);

            JsonNode playersRoot = objectMapper.readTree(playersResp.getBody());
            JsonNode playersArr = playersRoot.path("response");

            List<PlayerVO> squad = new ArrayList<>();

            for (JsonNode item : playersArr) {

                JsonNode playerNode = item.path("player");
                JsonNode statsArr = item.path("statistics");

                if (statsArr == null || statsArr.size() == 0) {
                    continue;
                }

                JsonNode statsNode = statsArr.get(0);

                PlayerVO p = new PlayerVO();

                // ---------- 基础信息 ----------
                Long playerId = playerNode.path("id").asLong();
                p.setId(playerId);

                p.setName(playerNode.path("name").asText());
                p.setAge(playerNode.path("age").isNull() ? null : playerNode.path("age").asInt());
                p.setPhoto(playerNode.path("photo").asText(null));

                // ---------- profile（缓存层🔥）----------
                PlayerProfile profile = playerProfileService.getProfileByPlayerId(playerId);

                if (profile != null) {
                    p.setNationality(profile.getNationality());
                    p.setPosition(profile.getPosition());
                    p.setNumber(profile.getNumber());
                } else {
                    p.setNationality(
                            playerNode.path("nationality").isMissingNode() || playerNode.path("nationality").isNull()
                                    ? null
                                    : playerNode.path("nationality").asText()
                    );
                }

                // ---------- stats ----------
                p.setAppearances(
                        statsNode.path("games").path("appearences").isNull()
                                ? null
                                : statsNode.path("games").path("appearences").asInt()
                );

                p.setGoals(
                        statsNode.path("goals").path("total").isNull()
                                ? null
                                : statsNode.path("goals").path("total").asInt()
                );

                p.setAssists(
                        statsNode.path("goals").path("assists").isNull()
                                ? null
                                : statsNode.path("goals").path("assists").asInt()
                );

                p.setRating(
                        statsNode.path("games").path("rating").isNull()
                                ? null
                                : statsNode.path("games").path("rating").asText()
                );

                squad.add(p);
            }

            // ---------- fixtures ----------

            String fixturesUrl =
                    "https://v3.football.api-sports.io/fixtures?team="
                            + teamId + "&season=" + season;

            ResponseEntity<String> fixturesResp =
                    restTemplate.exchange(fixturesUrl, HttpMethod.GET, entity, String.class);

            JsonNode fixturesRoot = objectMapper.readTree(fixturesResp.getBody());
            JsonNode fixturesArr = fixturesRoot.path("response");

            List<Fixture> fixtures = new ArrayList<>();

            for (JsonNode item : fixturesArr) {

                JsonNode fixtureNode = item.path("fixture");
                JsonNode leagueNode = item.path("league");
                JsonNode teamsNode = item.path("teams");
                JsonNode goalsNode = item.path("goals");

                JsonNode venueNode = fixtureNode.path("venue");
                JsonNode statusNode = fixtureNode.path("status");

                Fixture f = new Fixture();

                f.setId(fixtureNode.path("id").asLong());
                f.setLeagueId(leagueNode.path("id").asLong());

                // team ids
                f.setHomeTeamId(teamsNode.path("home").path("id").asLong());
                f.setAwayTeamId(teamsNode.path("away").path("id").asLong());

                // names
                f.setHomeTeamName(teamsNode.path("home").path("name").asText());
                f.setAwayTeamName(teamsNode.path("away").path("name").asText());

                // logos
                f.setHomeTeamLogo(teamsNode.path("home").path("logo").asText());
                f.setAwayTeamLogo(teamsNode.path("away").path("logo").asText());

                // score
                f.setHomeScore(goalsNode.path("home").isNull() ? null : goalsNode.path("home").asInt());
                f.setAwayScore(goalsNode.path("away").isNull() ? null : goalsNode.path("away").asInt());

                // status
                f.setStatus(statusNode.path("short").asText());
                f.setElapsed(statusNode.path("elapsed").isNull() ? null : statusNode.path("elapsed").asInt());

                f.setVenue(venueNode.path("name").asText(null));
                f.setReferee(fixtureNode.path("referee").asText(null));

                // match time
                f.setMatchTime(
                        OffsetDateTime
                                .parse(fixtureNode.path("date").asText())
                                .toLocalDateTime()
                );

                fixtures.add(f);
            }

            fixtures.sort(
                    Comparator.comparing(Fixture::getMatchTime).reversed()
            );

            // ---------- build vo ----------

            Team team = teamMapper.findById(teamId);

            TeamDetailVO vo = new TeamDetailVO();
            vo.setTeam(team);
            vo.setSquad(squad);
            vo.setFixtures(fixtures);

            return vo;

        } catch (Exception e) {

            throw new RuntimeException("fetchTeamDetail failed", e);
        }
    }

    @Override
    public PageResult<Player> fetchPlayersForQuery(
            Long teamId,
            Integer season,
            int page,
            int size
    ) {

        String json = fetchPlayers(teamId, season, page);

        try {

            JsonNode root = objectMapper.readTree(json);
            JsonNode response = root.get("response");

            List<Player> players = new ArrayList<>();

            for (JsonNode item : response) {

                JsonNode playerNode = item.get("player");

                Player player = new Player();

                player.setId(playerNode.get("id").asLong());
                player.setName(playerNode.get("name").asText());
                player.setAge(playerNode.get("age").asInt());
                player.setNationality(playerNode.get("nationality").asText());
                player.setPhoto(playerNode.get("photo").asText());

                players.add(player);
            }

            int start = (page - 1) * size;
            int end = Math.min(start + size, players.size());

            List<Player> pageList = players.subList(start, end);

            PageResult<Player> result = new PageResult<>();
            result.setTotal(players.size());
            result.setPage(page);
            result.setSize(size);
            result.setRecords(pageList);

            return result;

        } catch (Exception e) {
            throw new RuntimeException("fetchPlayersForQuery failed", e);
        }
    }

    @Override
    public PlayerDetailVO fetchPlayerDetail(Long playerId, Integer season) {

        String json = fetchPlayerById(playerId, season);

        try {

            JsonNode root = objectMapper.readTree(json);
            JsonNode response = root.get("response");

            if (response == null || response.isEmpty()) {
                return null;
            }

            JsonNode item = response.get(0);

            JsonNode playerNode = item.get("player");
            JsonNode statsNode = item.get("statistics").get(0);

            Player player = new Player();

            player.setId(playerNode.get("id").asLong());
            player.setName(playerNode.get("name").asText());
            player.setAge(playerNode.get("age").asInt());
            player.setNationality(playerNode.get("nationality").asText());
            player.setPhoto(playerNode.get("photo").asText());

            Team team = new Team();

            JsonNode teamNode = statsNode.get("team");

            team.setId(teamNode.get("id").asLong());
            team.setName(teamNode.get("name").asText());
            team.setLogo(teamNode.get("logo").asText());

            PlayerStats stats = new PlayerStats();

            stats.setSeason(season);

            JsonNode games = statsNode.get("games");
            JsonNode goals = statsNode.get("goals");

            if (games != null) {
                stats.setAppearances(games.get("appearences").asInt());
            }

            if (goals != null) {
                stats.setGoals(goals.get("total").asInt());
                stats.setAssists(goals.get("assists").asInt());
            }

            PlayerDetailVO vo = new PlayerDetailVO();

            vo.setPlayer(player);
            vo.setTeam(team);
            vo.setStatistics(List.of(stats));

            return vo;

        } catch (Exception e) {
            throw new RuntimeException("fetchPlayerDetail failed", e);
        }
    }



    private String fetchPlayers(Long teamId, Integer season, int page) {

        String url = "https://" + properties.getApi().getHost()
                + "/players?team=" + teamId
                + "&season=" + season
                + "&page=" + page;

        return restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(createHeaders()),
                String.class
        ).getBody();
    }

    private String fetchPlayerById(Long playerId, Integer season) {

        String url = "https://" + properties.getApi().getHost()
                + "/players?id=" + playerId
                + "&season=" + season;

        return restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(createHeaders()),
                String.class
        ).getBody();
    }

    private HttpHeaders createHeaders() {

        HttpHeaders headers = new HttpHeaders();

        headers.set("x-apisports-key", properties.getApi().getKey());
        headers.set("x-apisports-host", properties.getApi().getHost());

        return headers;
    }

    private Team fetchTeamInfo(Long teamId) {

        String url = "https://" + properties.getApi().getHost()
                + "/teams?id=" + teamId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-apisports-key", properties.getApi().getKey());
        headers.set("x-apisports-host", properties.getApi().getHost());

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
        );

        String body = response.getBody();

        try {

            JsonNode root = objectMapper.readTree(body);

            JsonNode resp = root.path("response");

            if (resp.isEmpty()) {
                throw new RuntimeException("Team not found: " + teamId);
            }

            JsonNode item = resp.get(0);

            JsonNode teamNode = item.path("team");
            JsonNode venueNode = item.path("venue");

            Team team = new Team();

            team.setId(teamNode.path("id").asLong());
            team.setName(teamNode.path("name").asText());
            team.setLogo(teamNode.path("logo").asText());
            team.setCountry(teamNode.path("country").asText(null));
            team.setFounded(teamNode.path("founded").asInt());

            team.setVenueName(venueNode.path("name").asText(null));
            team.setVenueCity(venueNode.path("city").asText(null));
            team.setVenueCapacity(venueNode.path("capacity").asInt());

            return team;

        } catch (Exception e) {
            throw new RuntimeException("fetchTeamInfo failed", e);
        }
    }

    private List<Player> fetchTeamSquad(Long teamId) {

        String url = "https://" + properties.getApi().getHost()
                + "/players/squads?team=" + teamId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-apisports-key", properties.getApi().getKey());
        headers.set("x-apisports-host", properties.getApi().getHost());

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
        );

        String body = response.getBody();

        try {

            JsonNode root = objectMapper.readTree(body);

            List<Player> players = new ArrayList<>();

            JsonNode squadNode = root.path("response").get(0).path("players");

            for (JsonNode p : squadNode) {

                Player player = new Player();

                player.setId(p.path("id").asLong());
                player.setName(p.path("name").asText());
                player.setAge(p.path("age").asInt());
                player.setNationality(p.path("nationality").asText(null));
                player.setPhoto(p.path("photo").asText(null));

                players.add(player);
            }

            return players;

        } catch (Exception e) {
            throw new RuntimeException("fetchTeamSquad failed", e);
        }
    }

    private String fetchFixtureStatistics(Long fixtureId) {

        String url = "https://" + properties.getApi().getHost()
                + "/fixtures/statistics?fixture=" + fixtureId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-apisports-key", properties.getApi().getKey());

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response =
                restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        return response.getBody();
    }

    private String fetchFixtureEvents(Long fixtureId) {

        String url = "https://" + properties.getApi().getHost()
                + "/fixtures/events?fixture=" + fixtureId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-apisports-key", properties.getApi().getKey());

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response =
                restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        return response.getBody();
    }

    private String fetchFixtureLineups(Long fixtureId) {

        String url = "https://" + properties.getApi().getHost()
                + "/fixtures/lineups?fixture=" + fixtureId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-apisports-key", properties.getApi().getKey());

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response =
                restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        return response.getBody();
    }

    private JsonNode findMatchById(JsonNode snapshot, long fixtureId) {

        for (JsonNode match : snapshot) {

            long id = match.get("fixture").get("id").asLong();

            if (id == fixtureId) {
                return match;
            }
        }

        return null;
    }

    private String textOrNull(JsonNode node) {
        if (node == null || node.isNull()) return null;
        String v = node.asText();
        return (v == null || v.isBlank()) ? null : v;
    }

    private Integer intOrNull(JsonNode node) {
        if (node == null || node.isNull()) return null;
        if (node.isInt()) return node.asInt();
        String v = node.asText();
        if (v == null || v.isBlank()) return null;
        try { return Integer.parseInt(v); } catch (Exception e) { return null; }
    }

    private Long longOrNull(JsonNode node) {
        if (node == null || node.isNull()) return null;
        if (node.isLong() || node.isInt()) return node.asLong();
        String v = node.asText();
        if (v == null || v.isBlank()) return null;
        try { return Long.parseLong(v); } catch (Exception e) { return null; }
    }

    private Integer boolOrZero(JsonNode node) {
        if (node == null || node.isNull()) return 0;
        return node.asBoolean(false) ? 1 : 0;
    }
}