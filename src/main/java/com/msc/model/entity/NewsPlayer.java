package com.msc.model.entity;

import lombok.Data;

@Data
public class NewsPlayer {

    private Long id;
    private Long newsId;
    private Long playerId;
}