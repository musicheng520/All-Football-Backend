package com.msc.model.vo.fixture;

import com.msc.model.entity.Fixture;
import com.msc.model.entity.Team;
import lombok.Data;

import java.util.List;

@Data
public class FixtureDetailVO {

    private Fixture fixture;

    private Team homeTeam;
    private Team awayTeam;

    private LineupVO homeLineup;
    private LineupVO awayLineup;

    private List<EventVO> events;

    private StatisticVO statistics;

}