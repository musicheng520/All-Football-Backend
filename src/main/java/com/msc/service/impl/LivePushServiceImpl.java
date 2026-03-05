package com.msc.service.impl;

import com.msc.service.LivePushService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LivePushServiceImpl implements LivePushService {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void broadcast(Object payload) {

        messagingTemplate.convertAndSend("/topic/live", payload);

    }
}