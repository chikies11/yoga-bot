package com.yogabot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yogabot.model.BotUser;
import com.yogabot.model.Schedule;
import com.yogabot.model.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SupabaseService {

    private static final Logger log = LoggerFactory.getLogger(SupabaseService.class);

    @Autowired
    private RestTemplate restTemplate;

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.key}")
    private String supabaseKey;

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", supabaseKey);
        headers.set("Authorization", "Bearer " + supabaseKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Prefer", "return=representation");
        return headers;
    }

    public String checkUserConnection() {
        try {
            String url = supabaseUrl + "/rest/v1/users?limit=1&select=*";
            HttpEntity<String> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            return response.getStatusCode().is2xxSuccessful() ? "✅ Connection OK" : "❌ Connection Failed";
        } catch (Exception e) {
            log.error("Supabase connection error", e);
            return "❌ Error: " + e.getMessage();
        }
    }

    // --- Schedule Methods ---

    public List<Schedule> getWeeklySchedule(LocalDate startOfWeek) {
        try {
            LocalDate endOfWeek = startOfWeek.plusDays(6);
            String query = String.format("date.gte.%s&date.lte.%s&order=date", startOfWeek, endOfWeek);
            String url = supabaseUrl + "/rest/v1/schedule?" + query;

            HttpEntity<String> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<Schedule[]> response = restTemplate.exchange(url, HttpMethod.GET, entity, Schedule[].class);

            return response.getBody() != null ? Arrays.asList(response.getBody()) : Collections.emptyList();
        } catch (Exception e) {
            log.error("Error getting weekly schedule", e);
            return Collections.emptyList();
        }
    }

    public Schedule getScheduleByDate(LocalDate date) {
        try {
            String url = supabaseUrl + "/rest/v1/schedule?date=eq." + date;
            HttpEntity<String> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<Schedule[]> response = restTemplate.exchange(url, HttpMethod.GET, entity, Schedule[].class);

            Schedule[] schedules = response.getBody();
            return (schedules != null && schedules.length > 0) ? schedules[0] : null;
        } catch (Exception e) {
            log.error("Error getting schedule by date: {}", date, e);
            return null;
        }
    }

    public void initializeDefaultSchedule() {
        LocalDate today = LocalDate.now();
        int daysToCover = 180;

        for (int i = 0; i < daysToCover; i++) {
            LocalDate date = today.plusDays(i);
            try {
                if (getScheduleByDate(date) == null) {
                    createSchedule(createDefaultSchedule(date));
                }
            } catch (Exception e) {
                log.error("Error initializing schedule for {}", date, e);
            }
        }
    }

    private Schedule createDefaultSchedule(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        Schedule schedule = new Schedule();
        schedule.setDate(date);
        schedule.setActive(true);

        switch (dayOfWeek) {
            case MONDAY: case WEDNESDAY: case THURSDAY: case FRIDAY: case SUNDAY:
                schedule.setMorningTime(LocalTime.of(8, 0));
                schedule.setMorningClass("МАЙСОР КЛАСС 8:00 - 11:30");
                schedule.setEveningTime(LocalTime.of(17, 0));
                schedule.setEveningClass("МАЙСОР КЛАСС 17:00 - 20:30");
                break;
            case TUESDAY:
                schedule.setMorningTime(LocalTime.of(8, 0));
                schedule.setMorningClass("МАЙСОР КЛАСС 8:00 - 11:30");
                break;
            case SATURDAY:
                schedule.setActive(false);
                schedule.setMorningClass("-Отдых-");
                break;
        }
        return schedule;
    }

    private void createSchedule(Schedule schedule) {
        try {
            String url = supabaseUrl + "/rest/v1/schedule";
            // Сериализуем вручную или через объект, если Jackson настроен корректно
            // Для надежности используем Map, как у вас было, но лучше использовать объект
            HttpEntity<Schedule> entity = new HttpEntity<>(schedule, createHeaders());
            restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            log.info("Created schedule for {}", schedule.getDate());
        } catch (Exception e) {
            log.error("Failed to create schedule", e);
        }
    }

    // --- User Methods ---

    public BotUser getBotUserByTelegramId(Long telegramId) {
        try {
            String url = supabaseUrl + "/rest/v1/users?telegram_id=eq." + telegramId;
            HttpEntity<String> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<BotUser[]> response = restTemplate.exchange(url, HttpMethod.GET, entity, BotUser[].class);
            BotUser[] users = response.getBody();
            return (users != null && users.length > 0) ? users[0] : null;
        } catch (Exception e) {
            log.error("Error getting user {}", telegramId, e);
            return null;
        }
    }

    // НОВЫЙ МЕТОД ДЛЯ ОПТИМИЗАЦИИ (N+1 fix)
    public List<BotUser> getUsersByIds(List<Long> telegramIds) {
        if (telegramIds == null || telegramIds.isEmpty()) return Collections.emptyList();

        try {
            String idsStr = telegramIds.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));

            // Используем оператор in для фильтрации
            String url = supabaseUrl + "/rest/v1/users?telegram_id=in.(" + idsStr + ")";

            HttpEntity<String> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<BotUser[]> response = restTemplate.exchange(url, HttpMethod.GET, entity, BotUser[].class);

            return response.getBody() != null ? Arrays.asList(response.getBody()) : Collections.emptyList();
        } catch (Exception e) {
            log.error("Error getting users by ids", e);
            return Collections.emptyList();
        }
    }

    public void saveOrUpdateBotUser(BotUser botUser) {
        try {
            // Упрощенная логика upsert (требуется настройка ON CONFLICT в Supabase,
            // но ваш метод через GET+POST/PATCH тоже рабочий, оставим его логику но чище)
            BotUser existing = getBotUserByTelegramId(botUser.getTelegramId());
            String url = supabaseUrl + "/rest/v1/users";

            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> map = new HashMap<>();
            map.put("telegram_id", botUser.getTelegramId());
            map.put("first_name", botUser.getFirstName());
            map.put("last_name", botUser.getLastName());
            map.put("username", botUser.getUsername());
            String json = mapper.writeValueAsString(map);

            if (existing != null) {
                String patchUrl = url + "?id=eq." + existing.getId();
                restTemplate.exchange(patchUrl, HttpMethod.PATCH, new HttpEntity<>(json, createHeaders()), String.class);
            } else {
                restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(json, createHeaders()), String.class);
            }
        } catch (Exception e) {
            log.error("Error saving user", e);
        }
    }

    // --- Subscription Methods ---

    public void subscribeToClass(Long telegramId, Long scheduleId, String classType, LocalDate classDate) {
        try {
            Subscription sub = new Subscription(telegramId, scheduleId, classType, classDate);
            // Добавляем заголовок для игнорирования дубликатов или используем on_conflict в БД
            HttpHeaders headers = createHeaders();
            headers.set("Prefer", "resolution=ignore-duplicates");

            HttpEntity<Subscription> entity = new HttpEntity<>(sub, headers);
            restTemplate.exchange(supabaseUrl + "/rest/v1/subscriptions", HttpMethod.POST, entity, String.class);
            log.info("User {} subscribed to {}", telegramId, scheduleId);
        } catch (Exception e) {
            log.error("Error subscribing", e);
            throw new RuntimeException("Subscription failed");
        }
    }

    public void unsubscribeFromClass(Long telegramId, Long scheduleId, String classType) {
        try {
            String query = String.format("telegram_id=eq.%d&schedule_id=eq.%d&class_type=eq.%s", telegramId, scheduleId, classType);
            String url = supabaseUrl + "/rest/v1/subscriptions?" + query;
            restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(createHeaders()), String.class);
        } catch (Exception e) {
            log.error("Error unsubscribing", e);
        }
    }

    public List<Subscription> getSubscriptionsForClass(Long scheduleId, String classType) {
        try {
            String query = String.format("schedule_id=eq.%d&class_type=eq.%s", scheduleId, classType);
            String url = supabaseUrl + "/rest/v1/subscriptions?" + query;
            ResponseEntity<Subscription[]> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(createHeaders()), Subscription[].class);
            return response.getBody() != null ? Arrays.asList(response.getBody()) : Collections.emptyList();
        } catch (Exception e) {
            log.error("Error getting subscriptions", e);
            return Collections.emptyList();
        }
    }
}