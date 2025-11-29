package com.yogabot.controller;

import com.yogabot.model.Schedule;
import com.yogabot.service.SupabaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
public class TestController {

    @Autowired
    private SupabaseService supabaseService;

    @PostMapping("/init-schedule")
    public String initializeSchedule() {
        try {
            supabaseService.initializeDefaultSchedule();
            return "Расписание успешно инициализировано!";
        } catch (Exception e) {
            return "Ошибка: " + e.getMessage();
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

            return "Найдено расписаний: " + schedules.size();
        } catch (Exception e) {
            return "Ошибка при проверке расписания: " + e.getMessage();
        }
    }
}