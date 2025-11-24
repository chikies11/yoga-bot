package com.yogabot.model;

import java.time.LocalDate;
import java.time.LocalTime;

public class Schedule {
    private Long id;
    private LocalDate date;
    private LocalTime morningTime;
    private String morningClass;
    private LocalTime eveningTime;
    private String eveningClass;
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