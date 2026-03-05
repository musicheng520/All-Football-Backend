package com.msc.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LineupPlayer {

    private Long id;
    private Long lineupId;
    private Long playerId;
    private String position;
    private Integer number;
    private Integer isStarting;
    private LocalDateTime createdAt;
}