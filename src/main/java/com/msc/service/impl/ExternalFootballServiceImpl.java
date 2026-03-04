package com.msc.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.msc.config.FootballProperties;
import com.msc.mapper.FixtureMapper;
import com.msc.mapper.PlayerMapper;
import com.msc.mapper.PlayerStatsMapper;
import com.msc.model.entity.Fixture;
import com.msc.model.entity.Player;
import com.msc.model.entity.PlayerStats;
import com.msc.model.entity.Team;
import com.msc.mapper.TeamMapper;
import com.msc.realtime.delta.DeltaManager;
import com.msc.service.ExternalFootballService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        for (Team team : teams) {

            String url = "https://" + properties.getApi().getHost()
                    + "/players/squads?team=" + team.getId();

            HttpHeaders headers = new HttpHeaders();
            headers.set("x-apisports-key", properties.getApi().getKey());

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response =
                    restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            try {

                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode responseArr = root.get("response");

                if (responseArr == null || responseArr.size() == 0) {
                    System.out.println("[PlayerSync] Empty squad for team=" + team.getId());
                    continue;
                }

                JsonNode playersArr = responseArr.get(0).get("players");

                int count = 0;

                for (JsonNode p : playersArr) {

                    Player player = new Player();

                    player.setId(p.get("id").asLong());
                    player.setName(p.get("name").asText());
                    player.setAge(intOrNull(p.get("age")));
                    player.setPhoto(textOrNull(p.get("photo")));

                    player.setTeamId(team.getId());
                    player.setLeagueId(team.getLeagueId());
                    player.setSeason(season);

                    player.setNationality(textOrNull(p.get("nationality")));

                    if (p.has("birth") && p.get("birth") != null) {
                        JsonNode birth = p.get("birth");

                        if (birth.has("date") && !birth.get("date").isNull()) {
                            player.setBirthDate(
                                    LocalDate.parse(birth.get("date").asText())
                            );
                        }

                        player.setBirthPlace(textOrNull(birth.get("place")));
                        player.setBirthCountry(textOrNull(birth.get("country")));
                    }

                    player.setHeight(textOrNull(p.get("height")));
                    player.setWeight(textOrNull(p.get("weight")));

                    playerMapper.upsert(player);
                    count++;
                }

                System.out.println("[PlayerSync] team=" + team.getId()
                        + " players=" + count);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        System.out.println("[PlayerSync] Completed season=" + season);
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
    public void refreshLiveSnapshotToRedis() {

        try {

            // 1️. read old snapshot
            String oldJson = stringRedisTemplate.opsForValue()
                    .get("live:fixtures");

            JsonNode oldSnapshot = null;
            if (oldJson != null && !oldJson.isEmpty()) {
                oldSnapshot = objectMapper.readTree(oldJson);
            }

            // 2️. get new snapshot FIRST
            String newJson = fetchLiveFixturesFilteredJson();
            JsonNode newSnapshot = objectMapper.readTree(newJson);

            // 3️. cold start handling
            if (oldSnapshot == null) {

                stringRedisTemplate.opsForValue()
                        .set("live:fixtures",
                                newJson,
                                Duration.ofSeconds(60));

                System.out.println("[LiveSnapshot] initialized (cold start)");
                return;
            }

            // 4️. construct old data Map（O(n)）
            Map<Long, JsonNode> oldMatchMap = new HashMap<>();

            for (JsonNode match : oldSnapshot) {

                long id = match.get("fixture")
                        .get("id")
                        .asLong();

                oldMatchMap.put(id, match);
            }

            // 5️. delta detection
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
                }
            }

            // 6️. update Redis snapshot
            stringRedisTemplate.opsForValue()
                    .set("live:fixtures",
                            newJson,
                            Duration.ofSeconds(60));

            System.out.println("[LiveSnapshot] updated");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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