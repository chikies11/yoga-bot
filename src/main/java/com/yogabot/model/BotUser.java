package com.yogabot.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BotUser {
    private Long id;

    @JsonProperty("telegram_id")
    private Long telegramId;

    @JsonProperty("first_name")
    private String firstName;

    @JsonProperty("last_name")
    private String lastName;

    private String username;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("updated_at")
    private String updatedAt;

    // Конструкторы
    public BotUser() {}

    public BotUser(Long telegramId, String firstName, String lastName, String username) {
        this.telegramId = telegramId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.username = username;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTelegramId() { return telegramId; }
    public void setTelegramId(Long telegramId) { this.telegramId = telegramId; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    // Полное имя для отображения
    public String getDisplayName() {
        if (username != null && !username.isEmpty()) {
            return "@" + username;
        } else if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if (firstName != null) {
            return firstName;
        } else {
            return "Пользователь " + telegramId;
        }
    }

    @Override
    public String toString() {
        return "BotUser{" +
                "id=" + id +
                ", telegramId=" + telegramId +
                ", firstName='" + firstName + '\'' +
                ", username='" + username + '\'' +
                '}';
    }
}