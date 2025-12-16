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

    public String getWeeklySchedule() {
        LocalDate today = LocalDate.now();
        List<Schedule> schedules = supabaseService.getWeeklySchedule(today);
        StringBuilder sb = new StringBuilder("📅 Расписание на неделю:\n\n");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

        if (schedules.isEmpty()) {
            return "❌ Расписание не найдено.";
        }

        for (Schedule schedule : schedules) {
            sb.append("🔸 ").append(getRussianDayName(schedule.getDate().getDayOfWeek()))
                    .append(", ").append(schedule.getDate().format(dateFormatter)).append(":\n");

            if (schedule.isActive() && (schedule.getMorningTime() != null || schedule.getEveningTime() != null)) {
                if (schedule.getMorningTime() != null) {
                    sb.append("   🌅 ").append(schedule.getMorningTime()).append(" - ").append(schedule.getMorningClass()).append("\n");
                }
                if (schedule.getEveningTime() != null) {
                    sb.append("   🌇 ").append(schedule.getEveningTime()).append(" - ").append(schedule.getEveningClass()).append("\n");
                }
            } else {
                sb.append("   😴 Занятий нет.\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    public SendMessage createNotificationMessage(LocalDate date) {
        Schedule schedule = supabaseService.getScheduleByDate(date);
        SendMessage message = new SendMessage();

        if (schedule == null || !schedule.isActive()) {
            message.setText("На завтра (" + date + ") занятий нет. Отдыхаем! 🧘‍♀️");
            return message;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("📣 Напоминание о занятиях!\n\n")
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

    // --- ОПТИМИЗИРОВАННЫЙ МЕТОД ---
    public String getSubscriptionsList(Long scheduleId, String classType) {
        // 1. Получаем список подписок
        List<Subscription> subscriptions = supabaseService.getSubscriptionsForClass(scheduleId, classType);

        if (subscriptions.isEmpty()) {
            return "На это занятие пока никто не записался.";
        }

        // 2. Собираем все ID пользователей
        List<Long> userIds = subscriptions.stream()
                .map(Subscription::getTelegramId)
                .distinct()
                .collect(Collectors.toList());

        // 3. Загружаем пользователей ОДНИМ запросом (Batch Fetch)
        List<BotUser> users = supabaseService.getUsersByIds(userIds);

        // 4. Создаем Map для быстрого поиска: ID -> User
        Map<Long, BotUser> userMap = users.stream()
                .collect(Collectors.toMap(BotUser::getTelegramId, user -> user, (u1, u2) -> u1));

        StringBuilder sb = new StringBuilder();
        String classTime = classType.equals("MORNING") ? "утреннее" : "вечернее";
        sb.append("📋 Записались на ").append(classTime).append(" занятие:\n\n");

        // 5. Формируем список в памяти
        for (int i = 0; i < subscriptions.size(); i++) {
            Subscription sub = subscriptions.get(i);
            BotUser user = userMap.get(sub.getTelegramId());
            String displayName = (user != null) ? user.getDisplayName() : "ID: " + sub.getTelegramId();

            sb.append(i + 1).append(". ").append(displayName).append("\n");
        }

        return sb.toString();
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