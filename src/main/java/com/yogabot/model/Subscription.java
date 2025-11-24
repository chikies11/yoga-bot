package com.yogabot.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Subscription {
    private Long id;
    private Long userId;
    private Long scheduleId;
    private String classType; // "MORNING" or "EVENING"
    private LocalDate classDate;
    private LocalDateTime subscribedAt;

    // constructors, getters, setters
    public Subscription() {}

    public Subscription(Long userId, Long scheduleId, String classType, LocalDate classDate) {
        this.userId = userId;
        this.scheduleId = scheduleId;
        this.classType = classType;
        this.classDate = classDate;
        this.subscribedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getScheduleId() { return scheduleId; }
    public void setScheduleId(Long scheduleId) { this.scheduleId = scheduleId; }

    public String getClassType() { return classType; }
    public void setClassType(String classType) { this.classType = classType; }

    public LocalDate getClassDate() { return classDate; }
    public void setClassDate(LocalDate classDate) { this.classDate = classDate; }

    public LocalDateTime getSubscribedAt() { return subscribedAt; }
    public void setSubscribedAt(LocalDateTime subscribedAt) { this.subscribedAt = subscribedAt; }
}