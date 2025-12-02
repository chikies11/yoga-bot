package com.yogabot.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;

import java.time.LocalDate;
import java.time.LocalTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Schedule {

    // Пробуем разные варианты аннотаций для ID
    @JsonProperty("id")
    private Long id;

    @JsonProperty("ID")
    private Long ID;

    @JsonProperty("Id")
    private Long Id;

    @JsonProperty("schedule_id")
    private Long scheduleId;

    @JsonProperty("_id")
    private Long _id;

    @JsonProperty("date")
    private LocalDate date;

    @JsonProperty("morning_time")
    private LocalTime morningTime;

    @JsonProperty("evening_time")
    private LocalTime eveningTime;

    @JsonProperty("morning_class")
    private String morningClass;

    @JsonProperty("evening_class")
    private String eveningClass;

    @JsonProperty("is_active")
    private Boolean active;

    // Getters and Setters
    public Long getRealId() {
        if (id != null) return id;
        if (ID != null) return ID;
        if (Id != null) return Id;
        if (scheduleId != null) return scheduleId;
        if (_id != null) return _id;
        return null;
    }

    // Обновите все геттеры и сеттеры
    public Long getId() {
        return getRealId(); // Используем метод для получения ID
    }

    public void setId(Long id) {
        this.id = id;
        this.ID = id;
        this.Id = id;
        this.scheduleId = id;
        this._id = id;
    }

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