package com.msc.model.vo;

import com.msc.model.entity.Player;
import com.msc.model.entity.PlayerStats;
import com.msc.model.entity.Team;
import lombok.Data;

import java.util.List;

@Data
public class PlayerDetailVO {

    private Player player;

    private Team team;

    private List<PlayerStats> statistics;

}