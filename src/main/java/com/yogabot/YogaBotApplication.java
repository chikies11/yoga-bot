package com.yogabot;

import com.yogabot.service.SupabaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

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
        try {
            System.out.println("Initializing default schedule...");
            supabaseService.initializeDefaultSchedule();
            System.out.println("Default schedule initialized successfully!");
        } catch (Exception e) {
            System.err.println("Error initializing schedule: " + e.getMessage());
            // Не прерываем запуск приложения если есть ошибки с БД
        }
    }
}