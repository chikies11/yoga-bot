package com.yogabot.controller;

import com.yogabot.model.Schedule;
import com.yogabot.service.SupabaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
public class TestController {

    @Autowired
    private SupabaseService supabaseService;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.key}")
    private String supabaseKey;

    @PostMapping("/force-init-schedule")
    public String forceInitializeSchedule() {
        try {
            // Создаем расписание на текущую неделю
            LocalDate today = LocalDate.now();
            LocalDate startOfWeek = today.minusDays(today.getDayOfWeek().getValue() - 1);

            for (int i = 0; i < 7; i++) {
                LocalDate date = startOfWeek.plusDays(i);
                Schedule schedule = createDefaultScheduleForDay(date);

                // Проверяем, существует ли уже расписание на эту дату
                Schedule existing = supabaseService.getScheduleByDate(date);
                if (existing == null) {
                    createScheduleInDb(schedule);
                } else {
                    // Если существует, обновляем
                    schedule.setId(existing.getId());
                    supabaseService.updateSchedule(schedule);
                }
            }
            return "Расписание успешно инициализировано согласно вашему формату!";
        } catch (Exception e) {
            return "Ошибка: " + e.getMessage();
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

    private void createScheduleInDb(Schedule schedule) {
        try {
            String url = supabaseUrl + "/rest/v1/schedule";

            HttpHeaders headers = new HttpHeaders();
            headers.set("apikey", supabaseKey);
            headers.set("Authorization", "Bearer " + supabaseKey);
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

            HttpEntity<Schedule> entity = new HttpEntity<>(schedule, headers);
            restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        } catch (Exception e) {
            System.err.println("Error creating schedule: " + e.getMessage());
        }
    }

    @GetMapping("/check-schedule")
    public String checkSchedule() {
        try {
            LocalDate startOfWeek = LocalDate.now().minusDays(LocalDate.now().getDayOfWeek().getValue() - 1);
            List<Schedule> schedules = supabaseService.getWeeklySchedule(startOfWeek);

            if (schedules.isEmpty()) {
                return "Расписание не найдено в БД";
            }

            StringBuilder result = new StringBuilder();
            result.append("Расписание в БД:\n\n");

            for (Schedule schedule : schedules) {
                result.append(schedule.getDate())
                        .append(": Утро=").append(schedule.getMorningClass())
                        .append(", Вечер=").append(schedule.getEveningClass())
                        .append("\n");
            }

            return result.toString();
        } catch (Exception e) {
            return "Ошибка при проверке расписания: " + e.getMessage();
        }
    }

    @GetMapping("/test-connection")
    public String testConnection() {
        boolean connected = supabaseService.testConnection();
        return connected ? "Supabase connection: OK" : "Supabase connection: FAILED";
    }
}