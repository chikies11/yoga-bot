package com.yogabot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.beans.factory.annotation.Autowired;
import com.yogabot.service.SupabaseService;
import javax.annotation.PostConstruct;

@SpringBootApplication
@EnableScheduling
public class YogaBotApplication {

    @Autowired
    private SupabaseService supabaseService;

    public static void main(String[] args) {
        SpringApplication.run(YogaBotApplication.class, args);
    }

    @PostConstruct
    public void initialize() {
        // Инициализируем расписание при запуске
        supabaseService.initializeDefaultSchedule();
    }
}