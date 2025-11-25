package com.yogabot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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

    @Bean
    public String getBotToken() {
        return botToken;
    }

    @Bean
    public String getBotUsername() {
        return botUsername;
    }

    @Bean
    public String getChannelId() {
        return channelId;
    }

    @Bean
    public Long getAdminTelegramId() {
        return adminTelegramId;
    }
}