package com.yogabot.controller;

import com.yogabot.service.BotService;
import com.yogabot.service.NotificationService;
import com.yogabot.service.SupabaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.time.LocalDate;
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

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private BotService botService;

    @PostMapping("/test-notification")
    public String testNotification() {
        try {
            notificationService.sendTestNotification();
            return "✅ Тестовое уведомление отправлено в канал!";
        } catch (Exception e) {
            return "❌ Ошибка отправки уведомления: " + e.getMessage();
        }
    }

    @GetMapping("/next-schedule")
    public String nextSchedule() {
        try {
            LocalDate tomorrow = LocalDate.now().plusDays(1);
            SendMessage message = botService.createNotificationMessage(tomorrow);
            return "Завтрашнее расписание: " + message.getText();
        } catch (Exception e) {
            return "Ошибка получения расписания: " + e.getMessage();
        }
    }
}