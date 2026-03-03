package com.msc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class AllFootballApplication {

    public static void main(String[] args) {
        SpringApplication.run(AllFootballApplication.class, args);
    }

}