package com.msc.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MatchEvent {

    private Long id;
    private Long fixtureId;
    private Long teamId;
    private Long playerId;
    private Long assistPlayerId;
    private Integer minute;
    private Integer extraMinute;
    private String type;
    private String detail;
    private String comments;
    private LocalDateTime createdAt;
}