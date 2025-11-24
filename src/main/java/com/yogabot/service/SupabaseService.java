package com.yogabot.service;

import com.yogabot.model.Schedule;
import com.yogabot.model.User;
import com.yogabot.model.Subscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class SupabaseService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Schedule methods
    public List<Schedule> getWeeklySchedule(LocalDate startOfWeek) {
        LocalDate endOfWeek = startOfWeek.plusDays(6);
        String sql = "SELECT * FROM schedule WHERE date BETWEEN ? AND ? ORDER BY date";
        return jdbcTemplate.query(sql, scheduleRowMapper, startOfWeek, endOfWeek);
    }

    public Schedule getScheduleByDate(LocalDate date) {
        String sql = "SELECT * FROM schedule WHERE date = ?";
        List<Schedule> schedules = jdbcTemplate.query(sql, scheduleRowMapper, date);
        return schedules.isEmpty() ? null : schedules.get(0);
    }

    public void updateSchedule(Schedule schedule) {
        String sql = "UPDATE schedule SET morning_time = ?, morning_class = ?, evening_time = ?, evening_class = ?, is_active = ? WHERE id = ?";
        jdbcTemplate.update(sql, schedule.getMorningTime(), schedule.getMorningClass(),
                schedule.getEveningTime(), schedule.getEveningClass(), schedule.isActive(), schedule.getId());
    }

    public void deleteSchedule(Long id) {
        String sql = "DELETE FROM schedule WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    public void initializeDefaultSchedule() {
        // Initialize default schedule for the current week
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.minusDays(today.getDayOfWeek().getValue() - 1);

        for (int i = 0; i < 7; i++) {
            LocalDate date = startOfWeek.plusDays(i);
            Schedule existing = getScheduleByDate(date);

            if (existing == null) {
                Schedule schedule = createDefaultScheduleForDay(date);
                String sql = "INSERT INTO schedule (date, morning_time, morning_class, evening_time, evening_class, is_active) VALUES (?, ?, ?, ?, ?, ?)";
                jdbcTemplate.update(sql, schedule.getDate(), schedule.getMorningTime(), schedule.getMorningClass(),
                        schedule.getEveningTime(), schedule.getEveningClass(), schedule.isActive());
            }
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
                return new Schedule(date, LocalTime.of(8, 0), "МАЙСОР КЛАСС",
                        LocalTime.of(17, 0), "МАЙСОР КЛАСС", true);
            case "TUESDAY":
                return new Schedule(date, LocalTime.of(8, 0), "МАЙСОР КЛАСС",
                        null, null, true);
            case "SATURDAY":
                return new Schedule(date, null, null, null, null, false);
            default:
                return new Schedule(date, null, null, null, null, false);
        }
    }

    // User methods
    public User getUserByTelegramId(Long telegramId) {
        String sql = "SELECT * FROM users WHERE telegram_id = ?";
        List<User> users = jdbcTemplate.query(sql, userRowMapper, telegramId);
        return users.isEmpty() ? null : users.get(0);
    }

    public void saveUser(User user) {
        String sql = "INSERT INTO users (telegram_id, first_name, last_name, username, is_admin) VALUES (?, ?, ?, ?, ?) " +
                "ON CONFLICT (telegram_id) DO UPDATE SET first_name = ?, last_name = ?, username = ?";
        jdbcTemplate.update(sql, user.getTelegramId(), user.getFirstName(), user.getLastName(),
                user.getUsername(), user.isAdmin(), user.getFirstName(), user.getLastName(), user.getUsername());
    }

    // Subscription methods
    public void subscribeToClass(Long userId, Long scheduleId, String classType, LocalDate classDate) {
        String sql = "INSERT INTO subscriptions (user_id, schedule_id, class_type, class_date) VALUES (?, ?, ?, ?) " +
                "ON CONFLICT (user_id, schedule_id, class_type) DO NOTHING";
        jdbcTemplate.update(sql, userId, scheduleId, classType, classDate);
    }

    public void unsubscribeFromClass(Long userId, Long scheduleId, String classType) {
        String sql = "DELETE FROM subscriptions WHERE user_id = ? AND schedule_id = ? AND class_type = ?";
        jdbcTemplate.update(sql, userId, scheduleId, classType);
    }

    public List<Subscription> getSubscriptionsForClass(Long scheduleId, String classType) {
        String sql = "SELECT s.*, u.first_name, u.last_name, u.username FROM subscriptions s " +
                "JOIN users u ON s.user_id = u.id WHERE s.schedule_id = ? AND s.class_type = ?";
        return jdbcTemplate.query(sql, subscriptionRowMapper, scheduleId, classType);
    }

    public boolean isUserSubscribed(Long userId, Long scheduleId, String classType) {
        String sql = "SELECT COUNT(*) FROM subscriptions WHERE user_id = ? AND schedule_id = ? AND class_type = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId, scheduleId, classType);
        return count != null && count > 0;
    }

    // Row mappers
    private final RowMapper<Schedule> scheduleRowMapper = (rs, rowNum) -> {
        Schedule schedule = new Schedule();
        schedule.setId(rs.getLong("id"));
        schedule.setDate(rs.getDate("date").toLocalDate());

        if (rs.getTime("morning_time") != null) {
            schedule.setMorningTime(rs.getTime("morning_time").toLocalTime());
        }
        schedule.setMorningClass(rs.getString("morning_class"));

        if (rs.getTime("evening_time") != null) {
            schedule.setEveningTime(rs.getTime("evening_time").toLocalTime());
        }
        schedule.setEveningClass(rs.getString("evening_class"));
        schedule.setActive(rs.getBoolean("is_active"));

        return schedule;
    };

    private final RowMapper<User> userRowMapper = (rs, rowNum) -> {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setTelegramId(rs.getLong("telegram_id"));
        user.setFirstName(rs.getString("first_name"));
        user.setLastName(rs.getString("last_name"));
        user.setUsername(rs.getString("username"));
        user.setAdmin(rs.getBoolean("is_admin"));
        return user;
    };

    private final RowMapper<Subscription> subscriptionRowMapper = (rs, rowNum) -> {
        Subscription subscription = new Subscription();
        subscription.setId(rs.getLong("id"));
        subscription.setUserId(rs.getLong("user_id"));
        subscription.setScheduleId(rs.getLong("schedule_id"));
        subscription.setClassType(rs.getString("class_type"));
        subscription.setClassDate(rs.getDate("class_date").toLocalDate());
        subscription.setSubscribedAt(rs.getTimestamp("subscribed_at").toLocalDateTime());
        return subscription;
    };
}