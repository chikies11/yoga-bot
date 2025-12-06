package com.yogabot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

// Аннотации @Bean удалены. Значения будут инжектироваться напрямую через @Value
@Configuration
public class BotConfig {

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.username}")
    private String botUsername;

    @Value("${telegram.channel.id}")
    private String channelId;

    @Value("${admin.telegram.id}")
    private Long adminTelegramId;

    public String getBotToken() {
        return botToken;
    }

    public String getBotUsername() {
        return botUsername;
    }

    public String getChannelId() {
        return channelId;
    }

    public Long getAdminTelegramId() {
        return adminTelegramId;
    }
}