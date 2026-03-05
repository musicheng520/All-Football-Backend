package com.msc.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msc.config.FootballProperties;
import com.msc.mapper.LineupMapper;
import com.msc.mapper.LineupPlayerMapper;
import com.msc.mapper.MatchEventMapper;
import com.msc.mapper.MatchStatisticMapper;
import com.msc.model.entity.Lineup;
import com.msc.model.entity.LineupPlayer;
import com.msc.model.entity.MatchEvent;
import com.msc.model.entity.MatchStatistic;
import com.msc.service.ExternalFootballService;
import com.msc.service.MatchFinalizeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MatchFinalizeServiceImpl implements MatchFinalizeService {


    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final FootballProperties properties;
    private final MatchEventMapper matchEventMapper;
    private final MatchStatisticMapper matchStatisticMapper;
    private final LineupMapper lineupMapper;
    private final LineupPlayerMapper lineupPlayerMapper;

    @Override
    @Transactional
    public void finalizeMatch(Long fixtureId) {

        try {

            String body = fetchFixtureById(fixtureId);
            JsonNode root = objectMapper.readTree(body);
            JsonNode responseArr = root.get("response");

            if (responseArr == null || responseArr.isEmpty()) {
                System.out.println("[Finalize] fixture=" + fixtureId + " empty response");
                return;
            }

            JsonNode match = responseArr.get(0);

            // 1) Clean old data
            lineupPlayerMapper.deleteByFixtureId(fixtureId);
            lineupMapper.deleteByFixtureId(fixtureId);
            matchEventMapper.deleteByFixtureId(fixtureId);
            matchStatisticMapper.deleteByFixtureId(fixtureId);

            // 2) Save statistics
            List<MatchStatistic> stats = parseStatistics(fixtureId, match.get("statistics"));
            if (!stats.isEmpty()) {
                matchStatisticMapper.insertBatch(stats);
            }

            // 3) Save events
            List<MatchEvent> events = parseEvents(fixtureId, match.get("events"));
            if (!events.isEmpty()) {
                matchEventMapper.insertBatch(events);
            }

            // 4) Save lineups + lineup players
            saveLineupsAndPlayers(fixtureId, match.get("lineups"));

            System.out.println("[Finalize] fixture=" + fixtureId
                    + " stats=" + stats.size()
                    + " events=" + events.size()
                    + " lineups_saved");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String fetchFixtureById(Long fixtureId) {

        String url = "https://" + properties.getApi().getHost()
                + "/fixtures?id=" + fixtureId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-apisports-key", properties.getApi().getKey());

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response =
                restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        return response.getBody();
    }
    private List<MatchStatistic> parseStatistics(Long fixtureId, JsonNode statisticsNode) {

        List<MatchStatistic> list = new ArrayList<>();

        if (statisticsNode == null || statisticsNode.isEmpty()) {
            return list;
        }

        for (JsonNode teamStats : statisticsNode) {

            JsonNode teamNode = teamStats.get("team");
            JsonNode statsArr = teamStats.get("statistics");

            if (teamNode == null || teamNode.get("id") == null) {
                continue;
            }

            Long teamId = teamNode.get("id").asLong();

            MatchStatistic s = new MatchStatistic();
            s.setFixtureId(fixtureId);
            s.setTeamId(teamId);

            if (statsArr != null && statsArr.isArray()) {

                for (JsonNode kv : statsArr) {

                    String type = kv.get("type").asText();
                    JsonNode value = kv.get("value");

                    if ("Total Shots".equals(type)) {
                        s.setShotsTotal(intOrNull(value));
                    } else if ("Shots on Goal".equals(type)) {
                        s.setShotsOnTarget(intOrNull(value));
                    } else if ("Ball Possession".equals(type)) {
                        s.setPossession(textOrNull(value));
                    } else if ("Fouls".equals(type)) {
                        s.setFouls(intOrNull(value));
                    } else if ("Corner Kicks".equals(type)) {
                        s.setCorners(intOrNull(value));
                    } else if ("Yellow Cards".equals(type)) {
                        s.setYellowCards(intOrNull(value));
                    } else if ("Red Cards".equals(type)) {
                        s.setRedCards(intOrNull(value));
                    } else if ("Offsides".equals(type)) {
                        s.setOffsides(intOrNull(value));
                    }
                }
            }

            list.add(s);
        }

        return list;
    }

    private List<MatchEvent> parseEvents(Long fixtureId, JsonNode eventsNode) {

        List<MatchEvent> list = new ArrayList<>();

        if (eventsNode == null || !eventsNode.isArray()) {
            return list;
        }

        for (JsonNode e : eventsNode) {

            MatchEvent me = new MatchEvent();
            me.setFixtureId(fixtureId);

            JsonNode team = e.get("team");
            if (team != null && team.get("id") != null) {
                me.setTeamId(team.get("id").asLong());
            }

            JsonNode player = e.get("player");
            if (player != null && player.get("id") != null) {
                me.setPlayerId(player.get("id").asLong());
            }

            JsonNode assist = e.get("assist");
            if (assist != null && assist.get("id") != null) {
                me.setAssistPlayerId(assist.get("id").asLong());
            }

            JsonNode time = e.get("time");
            if (time != null) {
                me.setMinute(intOrNull(time.get("elapsed")));
                me.setExtraMinute(intOrNull(time.get("extra")));
            }

            me.setType(textOrNull(e.get("type")));
            me.setDetail(textOrNull(e.get("detail")));
            me.setComments(textOrNull(e.get("comments")));

            list.add(me);
        }

        return list;
    }

    private void saveLineupsAndPlayers(Long fixtureId, JsonNode lineupsNode) {

        if (lineupsNode == null || !lineupsNode.isArray()) {
            return;
        }

        for (JsonNode lu : lineupsNode) {

            JsonNode team = lu.get("team");
            Long teamId = (team != null && team.get("id") != null)
                    ? team.get("id").asLong()
                    : null;

            Lineup lineup = new Lineup();
            lineup.setFixtureId(fixtureId);
            lineup.setTeamId(teamId);
            lineup.setFormation(textOrNull(lu.get("formation")));

            JsonNode coach = lu.get("coach");
            lineup.setCoach(coach != null ? textOrNull(coach.get("name")) : null);

            lineupMapper.insert(lineup); // will fill lineup.id

            Long lineupId = lineup.getId();

            List<LineupPlayer> players = new ArrayList<>();

            JsonNode startXI = lu.get("startXI");
            if (startXI != null && startXI.isArray()) {
                for (JsonNode p : startXI) {
                    JsonNode playerNode = p.get("player");
                    if (playerNode == null || playerNode.get("id") == null) {
                        continue;
                    }
                    LineupPlayer lp = new LineupPlayer();
                    lp.setLineupId(lineupId);
                    lp.setPlayerId(playerNode.get("id").asLong());
                    lp.setPosition(textOrNull(playerNode.get("pos")));
                    lp.setNumber(intOrNull(playerNode.get("number")));
                    lp.setIsStarting(1);
                    players.add(lp);
                }
            }

            JsonNode subs = lu.get("substitutes");
            if (subs != null && subs.isArray()) {
                for (JsonNode p : subs) {
                    JsonNode playerNode = p.get("player");
                    if (playerNode == null || playerNode.get("id") == null) {
                        continue;
                    }
                    LineupPlayer lp = new LineupPlayer();
                    lp.setLineupId(lineupId);
                    lp.setPlayerId(playerNode.get("id").asLong());
                    lp.setPosition(textOrNull(playerNode.get("pos")));
                    lp.setNumber(intOrNull(playerNode.get("number")));
                    lp.setIsStarting(0);
                    players.add(lp);
                }
            }

            if (!players.isEmpty()) {
                lineupPlayerMapper.insertBatch(players);
            }
        }
    }

    private Integer intOrNull(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String s = node.asText();
        if (s == null || s.isEmpty() || "null".equalsIgnoreCase(s)) {
            return null;
        }
        try {
            return Integer.parseInt(s.replace("%", "").trim());
        } catch (Exception e) {
            return null;
        }
    }

    private String textOrNull(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String s = node.asText();
        if (s == null || s.isEmpty() || "null".equalsIgnoreCase(s)) {
            return null;
        }
        return s;
    }
}