package com.msc.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MatchStatistic {

    private Long id;
    private Long fixtureId;
    private Long teamId;
    private Integer shotsTotal;
    private Integer shotsOnTarget;
    private String possession;
    private Integer fouls;
    private Integer corners;
    private Integer yellowCards;
    private Integer redCards;
    private Integer offsides;
    private LocalDateTime createdAt;
}