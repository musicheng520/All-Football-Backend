package com.msc.model.dto;

import lombok.Data;

@Data
public class CommentCreateDTO {

    private Long newsId;
    private String content;
}