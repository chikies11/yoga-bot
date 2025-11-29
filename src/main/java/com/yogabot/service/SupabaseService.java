package com.yogabot.service;

import com.yogabot.model.Schedule;
import com.yogabot.model.User;
import com.yogabot.model.Subscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Collections;

@Service
public class SupabaseService {

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
        return headers;
    }

    // Используемые методы:

    // Schedule methods
    public List<Schedule> getWeeklySchedule(LocalDate startOfWeek) {
        try {
            LocalDate endOfWeek = startOfWeek.plusDays(6);

            String query = String.format("date.gte.%s&date.lte.%s&order=date",
                    startOfWeek, endOfWeek);

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

    public Schedule getScheduleByDate(LocalDate date) {
        try {
            String query = String.format("date=eq.%s", date);
            String url = supabaseUrl + "/rest/v1/schedule?" + query;

            HttpEntity<String> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<Schedule[]> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, Schedule[].class);

            Schedule[] schedules = response.getBody();
            return schedules != null && schedules.length > 0 ? schedules[0] : null;
        } catch (Exception e) {
            System.err.println("Error getting schedule by date: " + e.getMessage());
            return null;
        }
    }

    public void initializeDefaultSchedule() {
        try {
            System.out.println("Starting schedule initialization...");

            LocalDate today = LocalDate.now();
            LocalDate startOfWeek = today.minusDays(today.getDayOfWeek().getValue() - 1);

            System.out.println("Initializing schedule for week starting from: " + startOfWeek);

            for (int i = 0; i < 7; i++) {
                LocalDate date = startOfWeek.plusDays(i);
                Schedule existing = getScheduleByDate(date);

                if (existing == null) {
                    Schedule schedule = createDefaultScheduleForDay(date);
                    createSchedule(schedule);
                    System.out.println("Created schedule for: " + date);
                } else {
                    System.out.println("Schedule already exists for: " + date);
                }
            }

            System.out.println("Schedule initialization completed!");

        } catch (Exception e) {
            System.err.println("Error in initializeDefaultSchedule: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Schedule createDefaultScheduleForDay(LocalDate date) {
        String dayOfWeek = date.getDayOfWeek().toString();

        switch (dayOfWeek) {
            case "MONDAY":
            case "WEDNESDAY":
            case "THURSDAY":
            case "FRIDAY":
            case "SUNDAY":
                // Не устанавливаем ID - он будет null
                return new Schedule(date, LocalTime.of(8, 0), "МАЙСОР КЛАСС 8:00 - 11:30",
                        LocalTime.of(17, 0), "МАЙСОР КЛАСС 17:00 - 20:30", true);
            case "TUESDAY":
                return new Schedule(date, LocalTime.of(8, 0), "МАЙСОР КЛАСС 8:00 - 11:30",
                        null, null, true);
            case "SATURDAY":
                return new Schedule(date, null, "-Отдых-", null, null, false);
            default:
                return new Schedule(date, null, "-Отдых-", null, null, false);
        }
    }

    private void createSchedule(Schedule schedule) {
        try {
            String url = supabaseUrl + "/rest/v1/schedule";

            HttpHeaders headers = createHeaders();
            HttpEntity<Schedule> entity = new HttpEntity<>(schedule, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);

            System.out.println("Schedule created successfully for: " + schedule.getDate());

        } catch (Exception e) {
            System.err.println("Error creating schedule for " + schedule.getDate() + ": " + e.getMessage());
            throw e;
        }
    }

    // User methods
    public User getUserByTelegramId(Long telegramId) {
        try {
            String query = String.format("telegram_id=eq.%d", telegramId);
            String url = supabaseUrl + "/rest/v1/users?" + query;

            HttpEntity<String> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<User[]> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, User[].class);

            User[] users = response.getBody();
            return users != null && users.length > 0 ? users[0] : null;
        } catch (Exception e) {
            System.err.println("Error getting user by telegram ID: " + e.getMessage());
            return null;
        }
    }

    public void saveUser(User user) {
        try {
            String url = supabaseUrl + "/rest/v1/users";

            HttpHeaders headers = createHeaders();
            headers.set("Prefer", "resolution=merge-duplicates");

            HttpEntity<User> entity = new HttpEntity<>(user, headers);
            restTemplate.exchange(url, HttpMethod.POST, entity, User.class);
        } catch (Exception e) {
            System.err.println("Error saving user: " + e.getMessage());
        }
    }

    // Subscription methods
    public void subscribeToClass(Long userId, Long scheduleId, String classType, LocalDate classDate) {
        try {
            Subscription subscription = new Subscription(userId, scheduleId, classType, classDate);
            String url = supabaseUrl + "/rest/v1/subscriptions";

            HttpHeaders headers = createHeaders();
            headers.set("Prefer", "resolution=merge-duplicates");

            HttpEntity<Subscription> entity = new HttpEntity<>(subscription, headers);
            restTemplate.exchange(url, HttpMethod.POST, entity, Subscription.class);
        } catch (Exception e) {
            System.err.println("Error subscribing to class: " + e.getMessage());
            throw e;
        }
    }

    public void unsubscribeFromClass(Long userId, Long scheduleId, String classType) {
        try {
            String query = String.format("user_id=eq.%d&schedule_id=eq.%d&class_type=eq.%s",
                    userId, scheduleId, classType);
            String url = supabaseUrl + "/rest/v1/subscriptions?" + query;

            HttpEntity<String> entity = new HttpEntity<>(createHeaders());
            restTemplate.exchange(url, HttpMethod.DELETE, entity, String.class);
        } catch (Exception e) {
            System.err.println("Error unsubscribing from class: " + e.getMessage());
            throw e;
        }
    }

    public List<Subscription> getSubscriptionsForClass(Long scheduleId, String classType) {
        try {
            String query = String.format("schedule_id=eq.%d&class_type=eq.%s",
                    scheduleId, classType);
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