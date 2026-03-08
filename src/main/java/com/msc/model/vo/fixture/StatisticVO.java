package com.msc.model.vo.fixture;

import com.msc.model.entity.MatchStatistic;
import lombok.Data;

@Data
public class StatisticVO {

    private MatchStatistic home;
    private MatchStatistic away;

}