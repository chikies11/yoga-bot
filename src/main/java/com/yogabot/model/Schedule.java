package com.yogabot.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonAnyGetter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.HashMap;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Schedule {

    // Основное поле ID - используем Integer вместо Long
    @JsonProperty("id")
    private Integer id;

    @JsonProperty("date")
    private LocalDate date;

    @JsonProperty("morning_time")
    @JsonDeserialize(using = LocalTimeDeserializer.class)
    @JsonSerialize(using = LocalTimeSerializer.class)
    private LocalTime morningTime;

    @JsonProperty("evening_time")
    @JsonDeserialize(using = LocalTimeDeserializer.class)
    @JsonSerialize(using = LocalTimeSerializer.class)
    private LocalTime eveningTime;

    @JsonProperty("morning_class")
    private String morningClass;

    @JsonProperty("evening_class")
    private String eveningClass;

    @JsonProperty("is_active")
    private Boolean active;

    // Дополнительные поля которые есть в БД но не нужны в логике
    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("updated_at")
    private String updatedAt;

    // Для отладки - сохраняем все полученные поля
    private Map<String, Object> additionalProperties = new HashMap<>();

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
    }

    // Getters and Setters - ИСПРАВЬТЕ ТИП ID на Integer
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    // Метод для получения ID как Long (для обратной совместимости)
    public Long getIdAsLong() {
        return id != null ? id.longValue() : null;
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

    // Getters and Setters для дополнительных полей
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    // Метод для захвата любых других полей
    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        additionalProperties.put(name, value);
        System.out.println("Captured field: " + name + " = " + value);
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return additionalProperties;
    }

    @Override
    public String toString() {
        return "Schedule{" +
                "id=" + id +
                ", date=" + date +
                ", morningTime=" + morningTime +
                ", morningClass='" + morningClass + '\'' +
                ", active=" + active +
                ", additionalFields=" + additionalProperties.keySet() +
                '}';
    }
}