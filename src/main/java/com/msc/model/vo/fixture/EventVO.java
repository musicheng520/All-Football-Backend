package com.msc.model.vo.fixture;

import lombok.Data;

@Data
public class EventVO {

    private Integer minute;
    private Long teamId;

    private Long playerId;
    private String playerName;

    private Long assistPlayerId;
    private String assistPlayerName;

    private String type;
    private String detail;

}