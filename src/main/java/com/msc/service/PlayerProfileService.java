package com.msc.service;

import com.msc.model.entity.PlayerProfile;

public interface PlayerProfileService {

    PlayerProfile getProfileByPlayerId(Long playerId);
}