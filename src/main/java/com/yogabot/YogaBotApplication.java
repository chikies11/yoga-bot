package com.yogabot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class YogaBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(YogaBotApplication.class, args);
    }

    // УБЕРИТЕ полностью этот метод пока не созданы таблицы
    // @PostConstruct
    // public void initialize() {
    //     supabaseService.initializeDefaultSchedule();
    // }
}