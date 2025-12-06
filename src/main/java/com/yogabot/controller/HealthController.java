package com.yogabot.controller;

import com.yogabot.model.Schedule;
import com.yogabot.service.NotificationService;
import com.yogabot.service.BotService;
import com.yogabot.service.SupabaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private SupabaseService supabaseService;

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

    @GetMapping("/debug-schedule-id")
    public String debugScheduleId() {
        try {
            LocalDate tomorrow = LocalDate.now().plusDays(1);
            Schedule schedule = supabaseService.getScheduleByDate(tomorrow);

            StringBuilder debug = new StringBuilder();
            debug.append("=== DEBUG SCHEDULE ID ===\n");
            debug.append("Date: ").append(tomorrow).append("\n");
            debug.append("Schedule: ").append(schedule).append("\n");

            if (schedule != null) {
                debug.append("ID: ").append(schedule.getId()).append("\n");
                debug.append("Morning: ").append(schedule.getMorningTime()).append("\n");
                debug.append("Evening: ").append(schedule.getEveningTime()).append("\n");
                debug.append("Active: ").append(schedule.getActive()).append("\n");
            } else {
                debug.append("Schedule is NULL\n");
            }

            return debug.toString();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @GetMapping("/force-init")
    public String forceInitSchedule() {
        try {
            LocalDate today = LocalDate.now();
            LocalDate startOfWeek = today.minusDays(today.getDayOfWeek().getValue() - 1);

            StringBuilder result = new StringBuilder();
            result.append("=== FORCE INITIALIZE SCHEDULE ===\n\n");

            for (int i = 0; i < 7; i++) {
                LocalDate date = startOfWeek.plusDays(i);
                result.append("Processing date: ").append(date).append("\n");

                // Создаем новое расписание
                Schedule schedule = new Schedule();
                schedule.setDate(date);
                schedule.setActive(true);

                if (date.getDayOfWeek().getValue() == 6) { // Суббота
                    schedule.setActive(false);
                    schedule.setMorningClass("-Отдых-");
                } else if (date.getDayOfWeek().getValue() == 2) { // Вторник
                    schedule.setMorningTime(LocalTime.of(8, 0));
                    schedule.setMorningClass("МАЙСОР КЛАСС 8:00 - 11:30");
                } else {
                    schedule.setMorningTime(LocalTime.of(8, 0));
                    schedule.setMorningClass("МАЙСОР КЛАСС 8:00 - 11:30");
                    schedule.setEveningTime(LocalTime.of(17, 0));
                    schedule.setEveningClass("МАЙСОР КЛАСС 17:00 - 20:30");
                }

                result.append("Schedule created: ").append(schedule.getMorningClass()).append("\n\n");
            }

            return result.toString();

        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @GetMapping("/check-db-structure")
    public String checkDbStructure() {
        // Этот метод ДОЛЖЕН БЫТЬ реализован в SupabaseService
        return supabaseService.checkDbStructureStatus();
    }

    @PostMapping("/create-test-schedule") // Предположим, это был этот метод
    public String initializeSchedule() {
        try {
            // Весь код инициализации находится в сервисе
            supabaseService.initializeDefaultSchedule();
            return "Расписание успешно инициализировано!";
        } catch (Exception e) {
            return "Ошибка при инициализации: " + e.getMessage();
        }
    }

    @GetMapping("/test-mapping")
    public String testMapping() {
        try {
            LocalDate date = LocalDate.of(2025, 12, 3);
            Schedule schedule = supabaseService.getScheduleByDate(date);

            if (schedule == null) {
                return "Schedule not found for date: " + date;
            }

            StringBuilder result = new StringBuilder();
            result.append("=== SCHEDULE MAPPING TEST ===\n\n");
            result.append("Date: ").append(date).append("\n");
            result.append("Schedule ID: ").append(schedule.getId()).append("\n");
            result.append("Schedule ID class: ").append(schedule.getId() != null ? schedule.getId().getClass() : "null").append("\n");
            result.append("ID as Long: ").append(schedule.getIdAsLong()).append("\n");
            result.append("Morning class: ").append(schedule.getMorningClass()).append("\n");
            result.append("Evening class: ").append(schedule.getEveningClass()).append("\n");
            result.append("Active: ").append(schedule.getActive()).append("\n");
            result.append("Additional properties keys: ").append(schedule.getAdditionalProperties().keySet()).append("\n");
            result.append("Full object: ").append(schedule.toString()).append("\n");

            return result.toString();

        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @GetMapping("/check-users-table")
    public String checkUsersTable() {
        // Логика проверки БД перенесена в SupabaseService
        return supabaseService.checkUserConnection();
    }
}