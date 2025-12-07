package com.yogabot.service;

import com.yogabot.model.BotUser;
import com.yogabot.model.Schedule;
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
import java.util.Map;
import java.util.stream.Collectors;

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
        // 1. Начинаем с СЕГОДНЯ
        LocalDate today = LocalDate.now();
        // 2. Получаем 7 записей расписания, начиная с сегодня.
        // Мы передаем LocalDate.now() в качестве начальной даты.
        List<Schedule> schedules = supabaseService.getWeeklySchedule(today);

        StringBuilder sb = new StringBuilder();
        sb.append("📅 Расписание на следующие 7 дней (начиная с сегодня):\n\n");

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

        // Если расписание пустое (в Supabase нет данных), выводим сообщение
        if (schedules.isEmpty()) {
            sb.append("❌ Расписание не найдено в базе данных. Пожалуйста, обратитесь к администратору.");
            return sb.toString();
        }

        // 3. Форматируем расписание
        for (Schedule schedule : schedules) {
            sb.append("🔸 ")
                    .append(getRussianDayName(schedule.getDate().getDayOfWeek()))
                    .append(", ")
                    .append(schedule.getDate().format(dateFormatter))
                    .append(":\n");

            // Проверяем, активен ли день или есть ли хоть одно занятие
            if (schedule.isActive() && (schedule.getMorningTime() != null || schedule.getEveningTime() != null)) {
                // Утро
                if (schedule.getMorningTime() != null) {
                    sb.append("   - Утро (")
                            .append(schedule.getMorningTime().format(DateTimeFormatter.ofPattern("HH:mm")))
                            .append("): ")
                            .append(schedule.getMorningClass())
                            .append("\n");
                }

                // Вечер
                if (schedule.getEveningTime() != null) {
                    sb.append("   - Вечер (")
                            .append(schedule.getEveningTime().format(DateTimeFormatter.ofPattern("HH:mm")))
                            .append("): ")
                            .append(schedule.getEveningClass())
                            .append("\n");
                }
            } else {
                // Отдых или неактивный день
                sb.append("   - Занятий нет.\n");
            }
            sb.append("\n"); // Добавляем пустую строку для разделения дней
        }

        return sb.toString();
    }

    public SendMessage createNotificationMessage(LocalDate date) {
        Schedule schedule = supabaseService.getScheduleByDate(date);
        SendMessage message = new SendMessage();

        if (schedule == null || (!schedule.getActive() && schedule.getMorningTime() == null && schedule.getEveningTime() == null)) {
            message.setText("На завтра занятий не запланировано. Отдыхаем и восстанавливаемся! 💫");
            return message;
        }

        // ДОБАВЬТЕ ОТЛАДОЧНЫЕ ВЫВОДЫ:
        System.out.println("🔍 Creating notification for date: " + date);
        System.out.println("   Schedule ID: " + schedule.getId());
        System.out.println("   Schedule ID type: " + (schedule.getId() != null ? schedule.getId().getClass() : "null"));
        System.out.println("   Additional props: " + schedule.getAdditionalProperties());

        // Получаем ID как Long
        Long scheduleId = null;
        if (schedule.getId() != null) {
            scheduleId = schedule.getId().longValue();
        } else if (schedule.getAdditionalProperties().containsKey("id")) {
            Object idObj = schedule.getAdditionalProperties().get("id");
            if (idObj instanceof Number) {
                scheduleId = ((Number) idObj).longValue();
                System.out.println("   Extracted ID from additionalProps: " + scheduleId);
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("📣 Напоминание о завтрашних занятиях:\n\n");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        String dayName = getRussianDayName(date.getDayOfWeek());
        sb.append("🗓 ").append(dayName).append(" (").append(date.format(formatter)).append(")\n\n");

        boolean hasMorning = schedule.getMorningTime() != null && schedule.getActive();
        boolean hasEvening = schedule.getEveningTime() != null && schedule.getActive();

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

        // Добавляем инлайн-кнопки для записи (если есть занятия И есть ID расписания)
        if ((hasMorning || hasEvening) && scheduleId != null) {
            System.out.println("✅ Adding inline buttons with scheduleId: " + scheduleId);

            InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            if (hasMorning) {
                List<InlineKeyboardButton> morningRow = new ArrayList<>();

                InlineKeyboardButton morningSubscribe = new InlineKeyboardButton();
                morningSubscribe.setText("📝 Записаться на утро");
                morningSubscribe.setCallbackData("subscribe_morning_" + scheduleId);

                InlineKeyboardButton morningUnsubscribe = new InlineKeyboardButton();
                morningUnsubscribe.setText("❌ Отменить утро");
                morningUnsubscribe.setCallbackData("unsubscribe_morning_" + scheduleId);

                morningRow.add(morningSubscribe);
                morningRow.add(morningUnsubscribe);
                rows.add(morningRow);
            }

            if (hasEvening) {
                List<InlineKeyboardButton> eveningRow = new ArrayList<>();

                InlineKeyboardButton eveningSubscribe = new InlineKeyboardButton();
                eveningSubscribe.setText("📝 Записаться на вечер");
                eveningSubscribe.setCallbackData("subscribe_evening_" + scheduleId);

                InlineKeyboardButton eveningUnsubscribe = new InlineKeyboardButton();
                eveningUnsubscribe.setText("❌ Отменить вечер");
                eveningUnsubscribe.setCallbackData("unsubscribe_evening_" + scheduleId);

                eveningRow.add(eveningSubscribe);
                eveningRow.add(eveningUnsubscribe);
                rows.add(eveningRow);
            }

            keyboardMarkup.setKeyboard(rows);
            message.setReplyMarkup(keyboardMarkup);
            System.out.println("✅ Successfully added inline buttons");
        } else if (hasMorning || hasEvening) {
            System.err.println("⚠️ Cannot add buttons - schedule ID is null");
            System.err.println("   hasMorning: " + hasMorning);
            System.err.println("   hasEvening: " + hasEvening);
            System.err.println("   scheduleId: " + scheduleId);
            System.err.println("   schedule.getId(): " + schedule.getId());
        }

        return message;
    }

    // Вспомогательный метод для текста без кнопок
    private String createNotificationText(Schedule schedule, LocalDate date) {
        StringBuilder sb = new StringBuilder();
        sb.append("📣 Напоминание о завтрашних занятиях:\n\n");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        String dayName = getRussianDayName(date.getDayOfWeek());
        sb.append("🗓 ").append(dayName).append(" (").append(date.format(formatter)).append(")\n\n");

        boolean hasMorning = schedule.getMorningTime() != null && schedule.getActive();
        boolean hasEvening = schedule.getEveningTime() != null && schedule.getActive();

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

        sb.append("\n⚠️ Функция записи временно недоступна");

        return sb.toString();
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
            // ИСПРАВЛЕНИЕ: используем getBotUserByTelegramId
            BotUser user = supabaseService.getBotUserByTelegramId(subscription.getTelegramId());

            if (user != null) {
                String userName = user.getDisplayName();
                sb.append(i + 1).append(". ").append(userName).append("\n");
            } else {
                sb.append(i + 1).append(". Пользователь ID: ").append(subscription.getTelegramId()).append("\n");
            }
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
            default: return dayOfWeek.toString();
        }
    }
}