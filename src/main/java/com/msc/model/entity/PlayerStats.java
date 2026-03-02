package com.msc.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PlayerStats {

    private Long id;

    private Long playerId;
    private Long teamId;
    private Long leagueId;
    private Integer season;

    private Integer appearances;
    private Integer goals;
    private Integer assists;
    private Integer yellowCards;
    private Integer redCards;
    private String rating;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}