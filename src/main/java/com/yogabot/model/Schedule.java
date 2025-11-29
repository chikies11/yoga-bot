package com.yogabot.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;

import java.time.LocalDate;
import java.time.LocalTime;

@JsonInclude(JsonInclude.Include.NON_NULL) // Исключает null поля из JSON
public class Schedule {

    @JsonProperty(access = JsonProperty.Access.READ_ONLY) // Только для чтения
    private Long id;

    @JsonProperty("date")
    private LocalDate date;

    @JsonProperty("morning_time")
    @JsonDeserialize(using = LocalTimeDeserializer.class)
    private LocalTime morningTime;

    @JsonProperty("evening_time")
    @JsonDeserialize(using = LocalTimeDeserializer.class)
    private LocalTime eveningTime;

    @JsonProperty("morning_class")
    private String morningClass;

    @JsonProperty("evening_class")
    private String eveningClass;

    @JsonProperty("is_active")
    private Boolean active;

    // Конструкторы
    public Schedule() {}

    public Schedule(LocalDate date, LocalTime morningTime, String morningClass,
                    LocalTime eveningTime, String eveningClass, Boolean active) {
        this.date = date;
        this.morningTime = morningTime;
        this.morningClass = morningClass;
        this.eveningTime = eveningTime;
        this.eveningClass = eveningClass;
        this.active = active;
        // ID не устанавливаем - будет null
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