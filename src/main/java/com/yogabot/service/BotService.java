package com.yogabot.service;

import com.yogabot.model.BotUser;
import com.yogabot.model.Schedule;
import com.yogabot.model.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BotService {

    private static final Logger log = LoggerFactory.getLogger(BotService.class);

    @Autowired
    private SupabaseService supabaseService;

    @Value("${admin.telegram.id}")
    private Long adminTelegramId;

    public boolean isAdmin(Long telegramId) {
        return telegramId != null && telegramId.equals(adminTelegramId);
    }

    // --- ЛОГИКА РАСПИСАНИЯ (7 ДНЕЙ) ---
    public String getWeeklySchedule() {
        LocalDate today = LocalDate.now();
        // Получаем расписание на 7 дней
        List<Schedule> schedules = supabaseService.getWeeklySchedule(today);
        StringBuilder sb = new StringBuilder("📅 <b>Расписание на ближайшие 7 дней:</b>\n\n");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

        if (schedules.isEmpty()) {
            return "❌ Расписание не найдено. Обратитесь к администратору для инициализации.";
        }

        for (Schedule schedule : schedules) {
            sb.append("🔸 <b>").append(getRussianDayName(schedule.getDate().getDayOfWeek()))
                    .append(", ").append(schedule.getDate().format(dateFormatter)).append(":</b>\n");

            if (schedule.isActive() && (schedule.getMorningTime() != null || schedule.getEveningTime() != null)) {
                if (schedule.getMorningTime() != null) {
                    sb.append("   🌅 ").append(schedule.getMorningTime()).append(" - ").append(schedule.getMorningClass()).append("\n");
                }
                if (schedule.getEveningTime() != null) {
                    sb.append("   🌇 ").append(schedule.getEveningTime()).append(" - ").append(schedule.getEveningClass()).append("\n");
                }
            } else {
                sb.append("   😴 Отдых / Занятий нет.\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    // --- НОВАЯ ЛОГИКА ДЛЯ КНОПКИ "ЗАПИСЬ" (СЕГОДНЯ + ЗАВТРА) ---
    public String getTodayTomorrowSubscriptions() {
        LocalDate today = LocalDate.now();
        StringBuilder sb = new StringBuilder("📋 <b>Список записавшихся (Сегодня и Завтра):</b>\n\n");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM");

        // Цикл на 2 дня: 0 (сегодня) и 1 (завтра)
        for (int i = 0; i < 2; i++) {
            LocalDate date = today.plusDays(i);
            Schedule schedule = supabaseService.getScheduleByDate(date);
            String dayLabel = (i == 0) ? "СЕГОДНЯ" : "ЗАВТРА";

            sb.append("🔹 <b>").append(dayLabel).append(" ")
                    .append(getRussianDayName(date.getDayOfWeek())).append(" (")
                    .append(date.format(dateFormatter)).append(")</b>\n");

            if (schedule == null || !schedule.isActive()) {
                sb.append("   <i>Занятий нет.</i>\n\n");
                continue;
            }

            // Утро
            if (schedule.getMorningTime() != null) {
                sb.append("   🌅 Утро (").append(schedule.getMorningTime()).append("): ").append(schedule.getMorningClass()).append("\n");
                String users = getFormattedUserList(schedule.getId(), "MORNING");
                sb.append(users).append("\n");
            }

            // Вечер
            if (schedule.getEveningTime() != null) {
                sb.append("   🌇 Вечер (").append(schedule.getEveningTime()).append("): ").append(schedule.getEveningClass()).append("\n");
                String users = getFormattedUserList(schedule.getId(), "EVENING");
                sb.append(users).append("\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    // Вспомогательный метод для получения списка имен (с оптимизацией N+1)
    private String getFormattedUserList(Long scheduleId, String classType) {
        if (scheduleId == null) return "   ⚠️ Ошибка ID расписания\n";

        List<Subscription> subscriptions = supabaseService.getSubscriptionsForClass(scheduleId, classType);

        if (subscriptions.isEmpty()) {
            return "      — <i>Нет записей</i>\n";
        }

        List<Long> userIds = subscriptions.stream()
                .map(Subscription::getTelegramId)
                .distinct()
                .collect(Collectors.toList());

        List<BotUser> users = supabaseService.getUsersByIds(userIds);
        Map<Long, BotUser> userMap = users.stream()
                .collect(Collectors.toMap(BotUser::getTelegramId, user -> user, (u1, u2) -> u1));

        StringBuilder userList = new StringBuilder();
        for (int i = 0; i < subscriptions.size(); i++) {
            Subscription sub = subscriptions.get(i);
            BotUser user = userMap.get(sub.getTelegramId());
            String displayName = (user != null) ? user.getDisplayName() : "ID: " + sub.getTelegramId();

            userList.append("      ").append(i + 1).append(". ").append(displayName).append("\n");
        }
        return userList.toString();
    }

    // Метод для отдельного списка (если нужно) - оставлен для совместимости
    public String getSubscriptionsList(Long scheduleId, String classType) {
        return getFormattedUserList(scheduleId, classType);
    }

    // --- УВЕДОМЛЕНИЯ ---
    public SendMessage createNotificationMessage(LocalDate date) {
        Schedule schedule = supabaseService.getScheduleByDate(date);
        SendMessage message = new SendMessage();
        message.setParseMode("HTML"); // Включаем HTML форматирование

        if (schedule == null || !schedule.isActive()) {
            message.setText("На завтра (" + date + ") занятий нет. Отдыхаем! 🧘‍♀️");
            return message;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("📣 <b>Напоминание о занятиях!</b>\n\n")
                .append("🗓 ").append(getRussianDayName(date.getDayOfWeek()))
                .append(" (").append(date.format(DateTimeFormatter.ofPattern("dd.MM"))).append(")\n\n");

        boolean hasMorning = schedule.getMorningTime() != null;
        boolean hasEvening = schedule.getEveningTime() != null;

        if (hasMorning) sb.append("🌅 Утро ").append(schedule.getMorningTime()).append(": ").append(schedule.getMorningClass()).append("\n");
        if (hasEvening) sb.append("🌇 Вечер ").append(schedule.getEveningTime()).append(": ").append(schedule.getEveningClass()).append("\n");

        message.setText(sb.toString());

        // Inline Buttons
        if (schedule.getId() != null) {
            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            if (hasMorning) {
                rows.add(createSubscribeRow("Утро", "morning", schedule.getId()));
            }
            if (hasEvening) {
                rows.add(createSubscribeRow("Вечер", "evening", schedule.getId()));
            }
            markup.setKeyboard(rows);
            message.setReplyMarkup(markup);
        }

        return message;
    }

    private List<InlineKeyboardButton> createSubscribeRow(String label, String type, Long scheduleId) {
        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton sub = new InlineKeyboardButton("📝 " + label);
        sub.setCallbackData("subscribe_" + type + "_" + scheduleId);

        InlineKeyboardButton unsub = new InlineKeyboardButton("❌ Отмена");
        unsub.setCallbackData("unsubscribe_" + type + "_" + scheduleId);

        row.add(sub);
        row.add(unsub);
        return row;
    }

    public String getRussianDayName(DayOfWeek dayOfWeek) {
        switch (dayOfWeek) {
            case MONDAY: return "Понедельник";
            case TUESDAY: return "Вторник";
            case WEDNESDAY: return "Среда";
            case THURSDAY: return "Четверг";
            case FRIDAY: return "Пятница";
            case SATURDAY: return "Суббота";
            case SUNDAY: return "Воскресенье";
            default: return "";
        }
    }
}