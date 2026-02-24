package com.msc.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Team {

    private Long id;

    private String name;
    private String shortName;
    private String code;

    private String country;
    private String countryCode;

    private Integer founded;
    private String logo;

    private Boolean isNational;

    private Long venueId;
    private String venueName;
    private String venueCity;
    private Integer venueCapacity;
    private String venueSurface;
    private String venueImage;

    private Long leagueId;
    private Integer season;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}