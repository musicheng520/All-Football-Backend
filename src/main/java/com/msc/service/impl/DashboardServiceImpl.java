package com.msc.service.impl;

import com.msc.mapper.*;
import com.msc.model.vo.DashboardSummaryVO;
import com.msc.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final UserMapper userMapper;
    private final TeamMapper teamMapper;
    private final PlayerMapper playerMapper;
    private final FixtureMapper fixtureMapper;
    private final NewsMapper newsMapper;
    private final CommentMapper commentMapper;
    private final FollowMapper followMapper;

    @Override
    public DashboardSummaryVO summary() {

        return new DashboardSummaryVO(
                userMapper.count(),
                teamMapper.count(),
                playerMapper.totalCount(),
                fixtureMapper.totalCount(),
                newsMapper.count(),
                commentMapper.count(),
                followMapper.totalCount()
        );
    }
}