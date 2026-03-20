package com.msc.model.entity;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class PlayerProfile {

    private Long playerId;

    private String firstName;
    private String lastName;

    private LocalDate birthDate;
    private String birthPlace;
    private String birthCountry;

    private String nationality;

    private String height;
    private String weight;

    private Integer number;
    private String position;

    private String photo;

    private LocalDateTime updatedAt;
}
