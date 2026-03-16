package com.msc.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Fixture {

    private Long id;

    private Long leagueId;
    private Integer season;
    private String round;

    private Long homeTeamId;
    private Long awayTeamId;

    private String homeTeamName;
    private String awayTeamName;

    private String homeTeamLogo;
    private String awayTeamLogo;

    private Integer homeScore;
    private Integer awayScore;

    private String status;      // NS, LIVE, FT, HT ...
    private Integer elapsed;    // minute

    private LocalDateTime matchTime;

    private String referee;
    private String venue;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}