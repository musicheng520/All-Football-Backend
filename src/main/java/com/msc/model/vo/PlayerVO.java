package com.msc.model.vo;

import lombok.Data;

@Data
public class PlayerVO {

    private Long id;
    private String name;
    private Integer age;

    private String photo;

    // profile
    private String position;
    private Integer number;
    private String nationality;

    // ===== stats =====
    private Integer appearances;
    private Integer goals;
    private Integer assists;
    private String rating;
}