package com.msc.model.vo.fixture;

import com.msc.model.entity.LineupPlayer;
import lombok.Data;

import java.util.List;

@Data
public class LineupVO {

    private Long teamId;
    private String formation;
    private String coach;

    private List<LineupPlayer> startingXI;
    private List<LineupPlayer> substitutes;

}