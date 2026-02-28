package com.msc.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardSummaryVO {

    private long userCount;
    private long teamCount;
    private long playerCount;
    private long fixtureCount;
    private long newsCount;
    private long commentCount;
    private long followCount;
}