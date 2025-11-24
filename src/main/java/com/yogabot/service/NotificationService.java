package com.yogabot.service;

import com.yogabot.controller.BotController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
public class NotificationService {

    @Autowired
    private BotService botService;

    @Autowired
    private BotController botController;

    private static final String CHANNEL_ID = "@your_channel_username"; // Замените на username вашего канала

    // Уведомление об утреннем занятии в 16:00 по Москве
    @Scheduled(cron = "0 0 16 * * ?", zone = "Europe/Moscow")
    public void sendMorningClassNotification() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        SendMessage message = botService.createNotificationMessage(tomorrow);
        message.setChatId(CHANNEL_ID);

        try {
            botController.execute(message);
            System.out.println("Sent morning notification at: " + LocalDateTime.now());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    // Уведомление о вечернем занятии в 16:01 по Москве
    @Scheduled(cron = "0 1 16 * * ?", zone = "Europe/Moscow")
    public void sendEveningClassNotification() {
        // Вечернее уведомление отправляется вместе с утренним в одном сообщении
        System.out.println("Evening notification scheduled at: " + LocalDateTime.now());
    }
}