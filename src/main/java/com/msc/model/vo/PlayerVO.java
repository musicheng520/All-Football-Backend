package com.msc.model.vo;

import lombok.Data;

@Data
public class PlayerVO {

    private Long id;
    private String name;
    private Integer age;
    private String nationality;
    private String photo;

    // ===== stats =====
    private Integer appearances;
    private Integer goals;
    private Integer assists;
    private String rating;
}
