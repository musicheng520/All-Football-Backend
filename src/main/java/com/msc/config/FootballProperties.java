package com.msc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "football")
public class FootballProperties {

    private Api api;
    private Integer defaultSeason;
    private List<Long> supportedLeagues;

    @Data
    public static class Api {
        private String key;
        private String host;
    }
}