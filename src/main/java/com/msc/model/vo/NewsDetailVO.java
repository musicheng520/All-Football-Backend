package com.msc.model.vo;

import com.msc.model.entity.News;
import com.msc.model.entity.Player;
import com.msc.model.entity.Team;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class NewsDetailVO {

    private Long id;
    private String title;
    private String content;
    private String authorName;
    private String category;
    private LocalDateTime publishedAt;

    private List<TeamSimpleVO> teams;
    private List<PlayerSimpleVO> players;
    private List<CommentVO> comments;
}