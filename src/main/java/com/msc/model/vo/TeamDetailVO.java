package com.msc.model.vo;

import com.msc.model.entity.Team;
import com.msc.model.entity.Player;
import com.msc.model.entity.Fixture;
import lombok.Data;

import java.util.List;

@Data
public class TeamDetailVO {

    private Team team;

    private List<PlayerVO> squad;

    private List<Fixture> fixtures;

}