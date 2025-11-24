package com.yogabot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import com.yogabot.controller.BotController;

@Configuration
public class BotConfig {

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.username}")
    private String botUsername;

    @Value("${admin.telegram.id}")
    private Long adminTelegramId;

    @Bean
    public TelegramBotsApi telegramBotsApi(BotController botController) throws TelegramApiException {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(botController);
        return botsApi;
    }

    @Bean
    public String getBotToken() {
        return botToken;
    }

    @Bean
    public String getBotUsername() {
        return botUsername;
    }

    @Bean
    public Long getAdminTelegramId() {
        return adminTelegramId;
    }
}