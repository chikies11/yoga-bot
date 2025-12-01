package com.yogabot.controller;

import com.yogabot.service.NotificationService;
import com.yogabot.service.BotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
public class HealthController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private BotService botService;

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

    // Изменим на GET для удобства тестирования через браузер
    @GetMapping("/test-notification")
    public String testNotification() {
        try {
            notificationService.sendTestNotification();
            return "✅ Тестовое уведомление отправлено в канал! Проверьте канал Telegram.";
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

    // Добавим endpoint для проверки канала
    @GetMapping("/check-channel")
    public String checkChannel() {
        try {
            return "Канал настроен на: " + notificationService.getChannelId();
        } catch (Exception e) {
            return "Ошибка получения информации о канале: " + e.getMessage();
        }
    }
}