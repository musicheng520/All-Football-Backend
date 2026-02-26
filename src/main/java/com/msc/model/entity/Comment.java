package com.msc.model.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Comment {

    private Long id;
    private Long newsId;
    private Long userId;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}