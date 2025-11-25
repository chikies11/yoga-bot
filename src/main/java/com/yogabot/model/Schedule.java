package com.yogabot.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.time.LocalTime;

public class Schedule {
    private Long id;

    @JsonProperty("date")
    private LocalDate date;

    @JsonProperty("morning_time")
    private LocalTime morningTime;

    @JsonProperty("morning_class")
    private String morningClass;

    @JsonProperty("evening_time")
    private LocalTime eveningTime;

    @JsonProperty("evening_class")
    private String eveningClass;

    @JsonProperty("is_active") // Должно совпадать с именем в БД
    private boolean isActive;

    // constructors, getters, setters
    public Schedule() {}

    public Schedule(LocalDate date, LocalTime morningTime, String morningClass,
                    LocalTime eveningTime, String eveningClass, boolean isActive) {
        this.date = date;
        this.morningTime = morningTime;
        this.morningClass = morningClass;
        this.eveningTime = eveningTime;
        this.eveningClass = eveningClass;
        this.isActive = isActive;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public LocalTime getMorningTime() { return morningTime; }
    public void setMorningTime(LocalTime morningTime) { this.morningTime = morningTime; }

    public String getMorningClass() { return morningClass; }
    public void setMorningClass(String morningClass) { this.morningClass = morningClass; }

    public LocalTime getEveningTime() { return eveningTime; }
    public void setEveningTime(LocalTime eveningTime) { this.eveningTime = eveningTime; }

    public String getEveningClass() { return eveningClass; }
    public void setEveningClass(String eveningClass) { this.eveningClass = eveningClass; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}