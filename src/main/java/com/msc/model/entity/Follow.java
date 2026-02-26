package com.msc.model.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Follow {

    private Long id;
    private Long userId;
    private Long teamId;
    private LocalDateTime createdAt;
}