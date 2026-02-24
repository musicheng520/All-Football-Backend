package com.msc.service.impl;

import com.msc.constant.MessageConstant;
import com.msc.exception.BusinessException;
import com.msc.exception.ResourceNotFoundException;
import com.msc.mapper.FixtureMapper;
import com.msc.mapper.PlayerMapper;
import com.msc.mapper.TeamMapper;
import com.msc.model.entity.Player;
import com.msc.result.PageResult;
import com.msc.service.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class PlayerServiceImpl implements PlayerService {

    private final PlayerMapper playerMapper;
    private final TeamMapper teamMapper;

    @Override
    public Player findById(Long id) {
        Player player = playerMapper.findById(id);
        if (player == null) {
            throw new ResourceNotFoundException(MessageConstant.NOT_FOUND);
        }
        return player;
    }

    @Override
    public void update(Player player) {

        if (playerMapper.findById(player.getId()) == null) {
            throw new ResourceNotFoundException(MessageConstant.NOT_FOUND);
        }

        playerMapper.update(player);
    }

    @Override
    public PageResult<Player> page(int page, int size, Long teamId, Integer season) {

        int offset = (page - 1) * size;

        long total = playerMapper.count(teamId, season);

        List<Player> list = playerMapper.findPage(offset, size, teamId, season);

        return new PageResult<>(total, page, size, list);
    }

    @Override
    public void delete(Long id) {

        if (playerMapper.findById(id) == null) {
            throw new ResourceNotFoundException(MessageConstant.NOT_FOUND);
        }

        playerMapper.delete(id);
    }

    @Override
    public void create(Player player) {

        // 1. check if it exists
        if (playerMapper.findById(player.getId()) != null) {
            throw new BusinessException(MessageConstant.ALREADY_EXISTS);
        }

        // 2. if it has team id , check if team exists
        if (player.getTeamId() != null) {
            if (teamMapper.findById(player.getTeamId()) == null) {
                throw new ResourceNotFoundException("Team not found");
            }
        }

        playerMapper.insert(player);
    }
}
