package com.yogabot.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Subscription {
    private Long id;

    @JsonProperty("telegram_id")
    private Long telegramId;

    @JsonProperty("schedule_id")
    private Long scheduleId;

    @JsonProperty("class_type")
    private String classType;

    @JsonProperty("class_date")
    private LocalDate classDate;

    @JsonProperty("subscribed_at")
    private LocalDateTime subscribedAt;

    // Конструктор
    public Subscription(Long telegramId, Long scheduleId, String classType, LocalDate classDate) {
        this.telegramId = telegramId;
        this.scheduleId = scheduleId;
        this.classType = classType;
        this.classDate = classDate;
        this.subscribedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTelegramId() { return telegramId; }
    public void setTelegramId(Long telegramId) { this.telegramId = telegramId; }

    public Long getScheduleId() { return scheduleId; }
    public void setScheduleId(Long scheduleId) { this.scheduleId = scheduleId; }

    public String getClassType() { return classType; }
    public void setClassType(String classType) { this.classType = classType; }

    public LocalDate getClassDate() { return classDate; }
    public void setClassDate(LocalDate classDate) { this.classDate = classDate; }

    public LocalDateTime getSubscribedAt() { return subscribedAt; }
    public void setSubscribedAt(LocalDateTime subscribedAt) { this.subscribedAt = subscribedAt; }
}