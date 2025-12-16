package com.yogabot.controller;

import com.yogabot.service.NotificationService;
import com.yogabot.service.SupabaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
public class HealthController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private SupabaseService supabaseService;

    @GetMapping("/health")
    public String health() {
        return "✅ Yoga Bot is alive! Time: " + LocalDateTime.now();
    }

    @GetMapping("/")
    public String home() {
        return "🧘 Yoga Bot is running!";
    }

    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }

    @GetMapping("/check-db")
    public String checkDb() {
        return supabaseService.checkUserConnection();
    }

    /*
     * ⚠️ ОПАСНЫЕ МЕТОДЫ (Отключены для безопасности в Production)
     * Раскомментируйте только для локального тестирования
     */

    // @GetMapping("/test-notification")
    // public String testNotification() {
    //     notificationService.sendTestNotification();
    //     return "Notification sent!";
    // }

    // @GetMapping("/force-init")
    // public String forceInit() {
    //     supabaseService.initializeDefaultSchedule();
    //     return "Schedule initialized!";
    // }
}