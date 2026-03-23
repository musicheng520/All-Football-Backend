package com.msc.model.dto;

import lombok.Data;
import java.util.List;

@Data
public class NewsCreateDTO {

    private String title;
    private String content;
    private String category;
    private String coverImage;
    private String images;
    private List<Long> teamIds;
    private List<Long> playerIds;
}