package com.msc.model.entity;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class Player {

    private Long id;

    private String name;
    private String firstName;
    private String lastName;

    private Integer age;

    private LocalDate birthDate;
    private String birthPlace;
    private String birthCountry;

    private String nationality;
    private String height;
    private String weight;
    private String photo;

    private Long teamId;
    private Long leagueId;
    private Integer season;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}