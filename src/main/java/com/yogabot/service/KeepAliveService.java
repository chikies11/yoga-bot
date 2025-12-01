package com.yogabot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import javax.annotation.PostConstruct;
import java.time.LocalDateTime;

@Service
public class KeepAliveService {

    @Value("${app.url:https://yoga-bot-ubxp.onrender.com}")
    private String appUrl;

    @Autowired
    private RestTemplate restTemplate; // Используем бин вместо new

    // Пинг каждые 14 минут
    @Scheduled(fixedRate = 14 * 60 * 1000)
    public void keepAlive() {
        try {
            String response = restTemplate.getForObject(appUrl + "/health", String.class);
            System.out.println("✅ Keep-alive ping successful: " + LocalDateTime.now());
            System.out.println("Response: " + response);
        } catch (Exception e) {
            System.err.println("❌ Keep-alive ping failed at " + LocalDateTime.now() + ": " + e.getMessage());
        }
    }

    // Дополнительный пинг каждые 5 минут
    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void frequentPing() {
        try {
            restTemplate.getForObject(appUrl + "/ping", String.class);
            System.out.println("🔔 Frequent ping: " + LocalDateTime.now());
        } catch (Exception e) {
            System.err.println("❌ Frequent ping failed: " + e.getMessage());
        }
    }

    @PostConstruct
    public void init() {
        System.out.println("🚀 Keep-alive service started at: " + LocalDateTime.now());
        // Сразу делаем первый пинг при запуске
        keepAlive();
    }
}