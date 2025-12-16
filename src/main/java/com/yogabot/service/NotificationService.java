package com.yogabot.service;

import com.yogabot.controller.BotController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class NotificationService {

    @Autowired
    private BotService botService;

    @Autowired
    private BotController botController;

    @Value("${telegram.channel.id}")
    private String channelId;

    // Флаг состояния уведомлений (по умолчанию включены)
    private boolean notificationsEnabled = true;

    // Метод переключения
    public String toggleNotifications() {
        notificationsEnabled = !notificationsEnabled;
        return notificationsEnabled ? "✅ Автоматические уведомления ВКЛЮЧЕНЫ." : "🔕 Автоматические уведомления ВЫКЛЮЧЕНЫ.";
    }

    public boolean isNotificationsEnabled() {
        return notificationsEnabled;
    }

    // Уведомление о занятиях на завтра в 16:00 по Москве
    @Scheduled(cron = "0 0 16 * * ?", zone = "Europe/Moscow")
    public void sendDailyNotification() {
        if (!notificationsEnabled) {
            System.out.println("🔕 Notifications are disabled. Skipping daily schedule sending.");
            return;
        }

        try {
            LocalDate tomorrow = LocalDate.now().plusDays(1);
            SendMessage message = botService.createNotificationMessage(tomorrow);
            message.setChatId(channelId);

            botController.execute(message);
            System.out.println("✅ Sent notification to channel at: " + LocalDateTime.now());

        } catch (Exception e) {
            System.err.println("❌ Error sending notification: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Тестовая отправка
    public void sendTestNotification() {
        try {
            LocalDate tomorrow = LocalDate.now().plusDays(1);
            SendMessage message = botService.createNotificationMessage(tomorrow);
            message.setChatId(channelId);
            botController.execute(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}