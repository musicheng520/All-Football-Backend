package com.msc.model.entity;


import lombok.Data;
import java.time.LocalDateTime;

@Data
public class News {

    private Long id;
    private String title;
    private String content;
    private Long authorId;
    private String category;

    private String coverImage;

    private String images;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}