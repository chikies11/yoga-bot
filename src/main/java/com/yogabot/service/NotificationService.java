package com.yogabot.service;

import com.yogabot.controller.BotController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
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

    // Уведомление о занятиях на завтра в 16:00 по Москве
    @Scheduled(cron = "0 0 16 * * ?", zone = "Europe/Moscow")
    public void sendDailyNotification() {
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

    // Дополнительная проверка в 16:01 (можно добавить логику если нужно)
    @Scheduled(cron = "0 1 16 * * ?", zone = "Europe/Moscow")
    public void sendEveningClassNotification() {
        System.out.println("🔔 Evening notification check at: " + LocalDateTime.now());
        // Можно добавить дополнительную логику если нужно
    }

    // Тестовый метод для ручной отправки уведомления
    public void sendTestNotification() {
        try {
            System.out.println("🔄 Starting test notification...");
            System.out.println("Channel ID: " + channelId);

            LocalDate tomorrow = LocalDate.now().plusDays(1);
            System.out.println("Tomorrow date: " + tomorrow);

            SendMessage message = botService.createNotificationMessage(tomorrow);
            message.setChatId(channelId);

            System.out.println("Message text: " + message.getText());
            System.out.println("Has reply markup: " + (message.getReplyMarkup() != null));

            botController.execute(message);
            System.out.println("✅ Test notification sent to channel successfully");

        } catch (Exception e) {
            System.err.println("❌ Error sending test notification: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to send test notification", e);
        }
    }

    public String getChannelId() {
        return channelId;
    }
}