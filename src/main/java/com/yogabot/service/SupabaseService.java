package com.yogabot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yogabot.model.BotUser;
import com.yogabot.model.Schedule;
import com.yogabot.model.Subscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Service
public class SupabaseService {

    @Autowired
    private RestTemplate restTemplate;

    // Свойства инжектируются напрямую, как и раньше
    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.key}")
    private String supabaseKey;

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", supabaseKey);
        headers.set("Authorization", "Bearer " + supabaseKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        // Добавляем заголовок Prefer для Supabase
        headers.set("Prefer", "return=representation");
        return headers;
    }

    public String checkUserConnection() {
        System.out.println("Executing Supabase connection check...");
        try {
            // Запрос, который просто проверяет, что таблица 'users' доступна
            String url = supabaseUrl + "/rest/v1/users?limit=1&select=*";

            HttpEntity<String> entity = new HttpEntity<>(createHeaders());

            // Выполнение запроса
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return "✅ Supabase connection successful! Status: " + response.getStatusCodeValue() +
                        ". Data length: " + response.getBody().length();
            } else {
                return "❌ Supabase connection failed. Status: " + response.getStatusCodeValue();
            }

        } catch (Exception e) {
            System.err.println("Error connecting to Supabase: " + e.getMessage());
            return "❌ Error connecting to Supabase: " + e.getMessage();
        }
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Методы Schedule (Исправлено имя таблицы: /rest/v1/schedule)
    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Возвращает расписание на 7 дней, начиная с указанной даты.
     */
    public List<Schedule> getWeeklySchedule(LocalDate startOfWeek) {
        try {
            // Фетчим 7 дней
            LocalDate endOfWeek = startOfWeek.plusDays(6);

            // ИСПРАВЛЕНИЕ: schedules -> schedule
            String query = String.format("date.gte.%s&date.lte.%s&order=date",
                    startOfWeek.toString(), endOfWeek.toString());
            String url = supabaseUrl + "/rest/v1/schedule?" + query;

            HttpEntity<String> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<Schedule[]> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, Schedule[].class);

            Schedule[] schedules = response.getBody();
            return schedules != null ? Arrays.asList(schedules) : Collections.emptyList();

        } catch (Exception e) {
            System.err.println("Error getting weekly schedule: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Возвращает расписание по конкретной дате.
     */
    public Schedule getScheduleByDate(LocalDate date) {
        try {
            String query = String.format("date=eq.%s", date.toString());
            // ИСПРАВЛЕНИЕ: schedules -> schedule
            String url = supabaseUrl + "/rest/v1/schedule?" + query;

            HttpEntity<String> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<Schedule[]> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, Schedule[].class);

            Schedule[] schedules = response.getBody();
            if (schedules != null && schedules.length > 0) {
                return schedules[0];
            }

            return null;

        } catch (Exception e) {
            System.err.println("Error getting schedule by date: " + e.getMessage());
            return null;
        }
    }

    public String checkDbStructureStatus() {
        try {
            // Используем параметры из application.properties
            String url = supabaseUrl + "/rest/v1/schedule?date=eq.2025-12-03&select=*";

            HttpHeaders headers = new HttpHeaders();
            headers.set("apikey", supabaseKey);
            headers.set("Authorization", "Bearer " + supabaseKey);
            headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));

            HttpEntity<String> entity = new HttpEntity<>(headers);
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);

            StringBuilder result = new StringBuilder();
            result.append("=== DATABASE STRUCTURE ===\n\n");
            result.append("URL: ").append(url).append("\n\n");
            result.append("Response Body:\n").append(response.getBody()).append("\n\n");
            result.append("Status: ").append(response.getStatusCode()).append("\n");

            return result.toString();

        } catch (Exception e) {
            return "Error: " + e.getMessage() + "\nSupabase URL: " + supabaseUrl + "\nSupabase Key: " + (supabaseKey != null ? "***" + supabaseKey.substring(Math.max(0, supabaseKey.length() - 5)) : "null");
        }
    }

    /**
     * Инициализирует расписание на следующие 180 дней, если оно отсутствует.
     */
    public void initializeDefaultSchedule() {
        LocalDate today = LocalDate.now();
        int daysToCover = 180;

        for (int i = 0; i < daysToCover; i++) {
            LocalDate date = today.plusDays(i);
            try {
                // 1. Проверяем, существует ли расписание на эту дату
                String checkQuery = String.format("date=eq.%s", date.toString());
                // ИСПРАВЛЕНИЕ: schedules -> schedule
                String checkUrl = supabaseUrl + "/rest/v1/schedule?" + checkQuery;

                HttpEntity<String> entity = new HttpEntity<>(createHeaders());
                ResponseEntity<Schedule[]> response = restTemplate.exchange(
                        checkUrl, HttpMethod.GET, entity, Schedule[].class);

                if (response.getBody() == null || response.getBody().length == 0) {
                    // 2. Если не существует, создаем дефолтное
                    Schedule newSchedule = createDefaultSchedule(date);

                    Map<String, Object> scheduleMap = new HashMap<>();
                    scheduleMap.put("date", newSchedule.getDate().toString());
                    scheduleMap.put("morning_time", newSchedule.getMorningTime() != null ? newSchedule.getMorningTime().toString() : null);
                    scheduleMap.put("morning_class", newSchedule.getMorningClass());
                    scheduleMap.put("evening_time", newSchedule.getEveningTime() != null ? newSchedule.getEveningTime().toString() : null);
                    scheduleMap.put("evening_class", newSchedule.getEveningClass());
                    scheduleMap.put("active", newSchedule.getActive());

                    String jsonBody = new ObjectMapper().writeValueAsString(scheduleMap);

                    HttpEntity<String> postEntity = new HttpEntity<>(jsonBody, createHeaders());
                    // ИСПРАВЛЕНИЕ: schedules -> schedule
                    String postUrl = supabaseUrl + "/rest/v1/schedule";

                    restTemplate.exchange(postUrl, HttpMethod.POST, postEntity, String.class);
                }
            } catch (Exception e) {
                System.err.println("❌ Error initializing schedule for " + date + ": " + e.getMessage());
            }
        }
    }

    /**
     * Создает объект Schedule с дефолтными значениями на основе дня недели.
     */
    private Schedule createDefaultSchedule(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        Schedule schedule = new Schedule();
        schedule.setDate(date);
        schedule.setActive(true);

        switch (dayOfWeek) {
            case MONDAY:
            case WEDNESDAY:
            case THURSDAY:
            case FRIDAY:
            case SUNDAY:
                schedule.setMorningTime(LocalTime.of(8, 0));
                schedule.setMorningClass("МАЙСОР КЛАСС 8:00 - 11:30");
                schedule.setEveningTime(LocalTime.of(17, 0));
                schedule.setEveningClass("МАЙСОР КЛАСС 17:00 - 20:30");
                break;
            case TUESDAY:
                schedule.setMorningTime(LocalTime.of(8, 0));
                schedule.setMorningClass("МАЙСОР КЛАСС 8:00 - 11:30");
                schedule.setEveningTime(null);
                schedule.setEveningClass(null);
                break;
            case SATURDAY:
                schedule.setActive(false); // Отдых
                schedule.setMorningTime(null);
                schedule.setMorningClass(null);
                schedule.setEveningTime(null);
                schedule.setEveningClass(null);
                break;
        }

        return schedule;
    }

    private Schedule createSchedule(Schedule schedule) {
        try {
            String url = supabaseUrl + "/rest/v1/schedule?select=*"; // Добавьте select=* чтобы получить созданную запись

            HttpHeaders headers = createHeaders();
            HttpEntity<Schedule> entity = new HttpEntity<>(schedule, headers);

            ResponseEntity<Schedule[]> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, Schedule[].class);

            Schedule[] result = response.getBody();
            if (result != null && result.length > 0) {
                Schedule created = result[0];
                System.out.println("✅ Schedule created successfully for: " + schedule.getDate() + " with ID: " + created.getId());
                return created;
            }
            return null;

        } catch (Exception e) {
            System.err.println("Error creating schedule for " + schedule.getDate() + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Методы BotUser (Используют users)
    // -----------------------------------------------------------------------------------------------------------------

    public BotUser getBotUserByTelegramId(Long telegramId) {
        try {
            String query = String.format("telegram_id=eq.%d", telegramId);
            String url = supabaseUrl + "/rest/v1/users?" + query; // Предполагая, что таблица называется users

            HttpEntity<String> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<BotUser[]> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, BotUser[].class);

            BotUser[] users = response.getBody();
            if (users != null && users.length > 0) {
                return users[0];
            }
            return null;
        } catch (Exception e) {
            System.err.println("Error getting user: " + e.getMessage());
            return null;
        }
    }

    public BotUser saveOrUpdateBotUser(BotUser botUser) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            // Используем только поля, которые нужно сохранить
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("telegram_id", botUser.getTelegramId());
            userMap.put("first_name", botUser.getFirstName());
            userMap.put("last_name", botUser.getLastName());
            userMap.put("username", botUser.getUsername());

            String jsonBody = mapper.writeValueAsString(userMap);
            String url = supabaseUrl + "/rest/v1/users"; // Предполагая, что таблица называется users

            // Если id есть, пытаемся обновить (PATCH)
            if (botUser.getId() != null) {
                String patchUrl = url + String.format("?id=eq.%d", botUser.getId());
                HttpEntity<String> patchEntity = new HttpEntity<>(jsonBody, createHeaders());
                restTemplate.exchange(patchUrl, HttpMethod.PATCH, patchEntity, String.class);
                return botUser;
            } else {
                // Если id нет, вставляем (POST)
                HttpEntity<String> postEntity = new HttpEntity<>(jsonBody, createHeaders());
                ResponseEntity<BotUser[]> response = restTemplate.exchange(
                        url, HttpMethod.POST, postEntity, BotUser[].class);

                BotUser[] createdUsers = response.getBody();
                if (createdUsers != null && createdUsers.length > 0) {
                    return createdUsers[0];
                }
                return botUser;
            }
        } catch (Exception e) {
            System.err.println("Error saving or updating user: " + e.getMessage());
            return null;
        }
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Методы Subscription (Используют subscriptions)
    // -----------------------------------------------------------------------------------------------------------------

    public void subscribeToClass(Long telegramId, Long scheduleId, String classType, LocalDate classDate) {
        try {
            Subscription subscription = new Subscription(telegramId, scheduleId, classType, classDate);
            String url = supabaseUrl + "/rest/v1/subscriptions";

            HttpHeaders headers = createHeaders();
            headers.set("Prefer", "resolution=merge-duplicates");

            HttpEntity<Subscription> entity = new HttpEntity<>(subscription, headers);
            restTemplate.exchange(url, HttpMethod.POST, entity, Subscription.class);

            System.out.println("✅ User subscribed: " + telegramId + " to schedule: " + scheduleId);

        } catch (Exception e) {
            System.err.println("Error subscribing: " + e.getMessage());
            throw e;
        }
    }

    public void unsubscribeFromClass(Long telegramId, Long scheduleId, String classType) {
        try {
            String query = String.format("telegram_id=eq.%d&schedule_id=eq.%d&class_type=eq.%s",
                    telegramId, scheduleId, classType);
            String url = supabaseUrl + "/rest/v1/subscriptions?" + query;

            HttpEntity<String> entity = new HttpEntity<>(createHeaders());
            restTemplate.exchange(url, HttpMethod.DELETE, entity, String.class);

            System.out.println("✅ User unsubscribed: " + telegramId);

        } catch (Exception e) {
            System.err.println("Error unsubscribing: " + e.getMessage());
            throw e;
        }
    }

    public List<Subscription> getSubscriptionsForClass(Long scheduleId, String classType) {
        try {
            String query = String.format("schedule_id=eq.%d&class_type=eq.%s", scheduleId, classType);
            String url = supabaseUrl + "/rest/v1/subscriptions?" + query;

            HttpEntity<String> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<Subscription[]> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, Subscription[].class);

            Subscription[] subscriptions = response.getBody();
            return subscriptions != null ? Arrays.asList(subscriptions) : Collections.emptyList();

        } catch (Exception e) {
            System.err.println("Error getting subscriptions: " + e.getMessage());
            return Collections.emptyList();
        }
    }
}