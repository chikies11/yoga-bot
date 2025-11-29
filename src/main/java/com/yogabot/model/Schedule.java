package com.yogabot.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.time.LocalTime;

public class Schedule {
    private Long id;

    @JsonProperty("date")
    private LocalDate date;

    @JsonProperty("morning_time")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime morningTime;

    @JsonProperty("morning_class")
    private String morningClass;

    @JsonProperty("evening_time")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime eveningTime;

    @JsonProperty("evening_class")
    private String eveningClass;

    @JsonProperty("is_active")
    private Boolean active;

    // constructors, getters, setters
    public Schedule() {}

    public Schedule(LocalDate date, LocalTime morningTime, String morningClass,
                    LocalTime eveningTime, String eveningClass, Boolean active) {
        this.date = date;
        this.morningTime = morningTime;
        this.morningClass = morningClass;
        this.eveningTime = eveningTime;
        this.eveningClass = eveningClass;
        this.active = active;
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

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    // Для обратной совместимости
    public Boolean isActive() { return active; }
}