package com.msc.service.impl;

import com.msc.constant.MessageConstant;
import com.msc.exception.BusinessException;
import com.msc.exception.ResourceNotFoundException;
import com.msc.mapper.TeamMapper;
import com.msc.model.entity.Team;
import com.msc.result.PageResult;
import com.msc.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TeamServiceImpl implements TeamService {

    private final TeamMapper teamMapper;

    @Override
    public Team findById(Long id) {
        return teamMapper.findById(id);
    }

    @Override
    public void delete(Long id) {
        Team existing = teamMapper.findById(id);
        if (existing == null) {
            throw new ResourceNotFoundException(MessageConstant.NOT_FOUND);
        }
        teamMapper.delete(id);
    }



    @Override
    public void update(Team team) {
        Team existing = teamMapper.findById(team.getId());
        if (existing == null) {
            throw new ResourceNotFoundException(MessageConstant.NOT_FOUND);
        }
        teamMapper.update(team);
    }

    @Override
    public void create(Team team) {
        Team existing = teamMapper.findById(team.getId());
        if (existing != null) {
            throw new BusinessException(MessageConstant.ALREADY_EXISTS);
        }
        teamMapper.insert(team);
    }

    @Override
    public PageResult<Team> page(int page, int size) {

        int offset = (page - 1) * size;

        long total = teamMapper.count();
        List<Team> list = teamMapper.findPage(offset, size);

        return new PageResult<>(total, page, size, list);
    }
}
