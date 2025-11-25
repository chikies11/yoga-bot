package com.yogabot.service;

import com.yogabot.model.Schedule;
import com.yogabot.model.User;
import com.yogabot.model.Subscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;

@Service
public class BotService {

    @Autowired
    private SupabaseService supabaseService;

    @Value("${admin.telegram.id}")
    private Long adminTelegramId;

    public boolean isAdmin(Long telegramId) {
        return telegramId.equals(adminTelegramId);
    }

    public String getWeeklySchedule() {
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.minusDays(today.getDayOfWeek().getValue() - 1);
        List<Schedule> schedules = supabaseService.getWeeklySchedule(startOfWeek);

        StringBuilder sb = new StringBuilder();
        sb.append("📅 Расписание на текущую неделю:\n\n");

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        for (Schedule schedule : schedules) {
            String dayName = getRussianDayName(schedule.getDate().getDayOfWeek());
            sb.append("🗓 ").append(dayName).append(" (").append(schedule.getDate().format(dateFormatter)).append("):\n");

            if (schedule.getMorningTime() != null) {
                sb.append("🌅 Утро: ").append(schedule.getMorningTime().format(timeFormatter))
                        .append(" - ").append(schedule.getMorningClass()).append("\n");
            }

            if (schedule.getEveningTime() != null) {
                sb.append("🌇 Вечер: ").append(schedule.getEveningTime().format(timeFormatter))
                        .append(" - ").append(schedule.getEveningClass()).append("\n");
            }

            if (schedule.getMorningTime() == null && schedule.getEveningTime() == null) {
                sb.append("💤 Отдых\n");
            }

            sb.append("\n");
        }

        return sb.toString();
    }

    public SendMessage createNotificationMessage(LocalDate date) {
        Schedule schedule = supabaseService.getScheduleByDate(date);
        SendMessage message = new SendMessage();

        if (schedule == null || (!schedule.isActive() && schedule.getMorningTime() == null && schedule.getEveningTime() == null)) {
            message.setText("На завтра занятий не запланировано. Отдыхаем и восстанавливаемся! 💫");
            return message;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("📣 Напоминание о завтрашних занятиях:\n\n");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        String dayName = getRussianDayName(date.getDayOfWeek());
        sb.append("🗓 ").append(dayName).append(" (").append(date.format(formatter)).append(")\n\n");

        boolean hasMorning = schedule.getMorningTime() != null && schedule.isActive();
        boolean hasEvening = schedule.getEveningTime() != null && schedule.isActive();

        if (hasMorning) {
            sb.append("🌅 Утреннее занятие:\n");
            sb.append("⏰ ").append(schedule.getMorningTime()).append("\n");
            sb.append("🧘 ").append(schedule.getMorningClass()).append("\n\n");
        }

        if (hasEvening) {
            sb.append("🌇 Вечернее занятие:\n");
            sb.append("⏰ ").append(schedule.getEveningTime()).append("\n");
            sb.append("🧘 ").append(schedule.getEveningClass()).append("\n");
        }

        message.setText(sb.toString());

        // Добавляем кнопки только если есть активные занятия
        if (hasMorning || hasEvening) {
            InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            if (hasMorning) {
                List<InlineKeyboardButton> morningRow = new ArrayList<>();
                InlineKeyboardButton morningSubscribe = new InlineKeyboardButton();
                morningSubscribe.setText("📝 Записаться на утро");
                morningSubscribe.setCallbackData("subscribe_morning_" + schedule.getId());

                InlineKeyboardButton morningUnsubscribe = new InlineKeyboardButton();
                morningUnsubscribe.setText("❌ Отменить утро");
                morningUnsubscribe.setCallbackData("unsubscribe_morning_" + schedule.getId());

                morningRow.add(morningSubscribe);
                morningRow.add(morningUnsubscribe);
                rows.add(morningRow);
            }

            if (hasEvening) {
                List<InlineKeyboardButton> eveningRow = new ArrayList<>();
                InlineKeyboardButton eveningSubscribe = new InlineKeyboardButton();
                eveningSubscribe.setText("📝 Записаться на вечер");
                eveningSubscribe.setCallbackData("subscribe_evening_" + schedule.getId());

                InlineKeyboardButton eveningUnsubscribe = new InlineKeyboardButton();
                eveningUnsubscribe.setText("❌ Отменить вечер");
                eveningUnsubscribe.setCallbackData("unsubscribe_evening_" + schedule.getId());

                eveningRow.add(eveningSubscribe);
                eveningRow.add(eveningUnsubscribe);
                rows.add(eveningRow);
            }

            keyboardMarkup.setKeyboard(rows);
            message.setReplyMarkup(keyboardMarkup);
        }

        return message;
    }

    public String getSubscriptionsList(Long scheduleId, String classType) {
        List<Subscription> subscriptions = supabaseService.getSubscriptionsForClass(scheduleId, classType);

        if (subscriptions.isEmpty()) {
            return "На это занятие пока никто не записался.";
        }

        StringBuilder sb = new StringBuilder();
        String classTime = classType.equals("MORNING") ? "утреннее" : "вечернее";
        sb.append("📋 Список записавшихся на ").append(classTime).append(" занятие:\n\n");

        for (int i = 0; i < subscriptions.size(); i++) {
            Subscription subscription = subscriptions.get(i);
            User user = supabaseService.getUserByTelegramId(subscription.getUserId());
            if (user != null) {
                String userName = user.getUsername() != null ? "@" + user.getUsername() :
                        user.getFirstName() + " " + user.getLastName();
                sb.append(i + 1).append(". ").append(userName).append("\n");
            }
        }

        return sb.toString();
    }

    private String getRussianDayName(DayOfWeek dayOfWeek) {
        switch (dayOfWeek) {
            case MONDAY: return "Понедельник";
            case TUESDAY: return "Вторник";
            case WEDNESDAY: return "Среда";
            case THURSDAY: return "Четверг";
            case FRIDAY: return "Пятница";
            case SATURDAY: return "Суббота";
            case SUNDAY: return "Воскресенье";
            default: return dayOfWeek.toString();
        }
    }
}