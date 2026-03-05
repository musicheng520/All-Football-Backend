package com.msc.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Lineup {

    private Long id;
    private Long fixtureId;
    private Long teamId;
    private String formation;
    private String coach;
    private LocalDateTime createdAt;
}