package com.yogabot.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDateTime;

@RestController
public class HealthController {

    @GetMapping("/health")
    public String health() {
        return "✅ Yoga Bot is alive! Time: " + LocalDateTime.now();
    }

    @GetMapping("/")
    public String home() {
        return "🧘 Yoga Bot is running! Server time: " + LocalDateTime.now();
    }

    @GetMapping("/ping")
    public String ping() {
        return "pong - " + LocalDateTime.now();
    }
}