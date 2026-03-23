package com.msc.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommentVO {
    private Long id;
    private String content;
    private String username;
    private String avatar;
    private LocalDateTime createdAt;
}
