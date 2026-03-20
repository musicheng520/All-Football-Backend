package com.msc.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msc.config.FootballProperties;
import com.msc.mapper.PlayerProfileMapper;
import com.msc.model.entity.PlayerProfile;
import com.msc.service.PlayerProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.Properties;

@Service
@RequiredArgsConstructor
public class PlayerProfileServiceImpl implements PlayerProfileService {

    private final PlayerProfileMapper playerProfileMapper;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final FootballProperties properties; // 你原来的配置

    @Override
    public PlayerProfile getProfileByPlayerId(Long playerId) {

        //  1. 查数据库
        PlayerProfile profile = playerProfileMapper.findByPlayerId(playerId);

        if (profile != null) {
            return profile;
        }

        //  2. DB没有 → 调API
        try {

            HttpHeaders headers = new HttpHeaders();
            headers.set("x-apisports-key", properties.getApi().getKey());

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String url =
                    "https://v3.football.api-sports.io/players/profiles?player=" + playerId;

            ResponseEntity<String> resp =
                    restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            JsonNode root = objectMapper.readTree(resp.getBody());
            JsonNode arr = root.path("response");

            if (arr.isEmpty()) {
                return null;
            }

            JsonNode playerNode = arr.get(0).path("player");

            // 3. 构建 entity
            PlayerProfile p = new PlayerProfile();

            p.setPlayerId(playerNode.path("id").asLong());
            p.setFirstName(playerNode.path("firstname").asText(null));
            p.setLastName(playerNode.path("lastname").asText(null));

            String birthDateStr = playerNode.path("birth").path("date").asText(null);

            p.setBirthDate(
                    birthDateStr == null ? null : LocalDate.parse(birthDateStr)
            );
            p.setBirthPlace(playerNode.path("birth").path("place").asText(null));
            p.setBirthCountry(playerNode.path("birth").path("country").asText(null));

            p.setNationality(playerNode.path("nationality").asText(null));
            p.setHeight(playerNode.path("height").asText(null));
            p.setWeight(playerNode.path("weight").asText(null));
            p.setNumber(playerNode.path("number").isNull() ? null : playerNode.path("number").asInt());
            p.setPosition(playerNode.path("position").asText(null));
            p.setPhoto(playerNode.path("photo").asText(null));

            // 4. 写入数据库（缓存）
            playerProfileMapper.insertOrUpdate(p);

            return p;

        } catch (Exception e) {
            // 容错：不能影响主流程
            return null;
        }
    }
}