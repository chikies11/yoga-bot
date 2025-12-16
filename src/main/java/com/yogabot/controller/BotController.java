package com.yogabot.controller;

import com.yogabot.model.BotUser;
import com.yogabot.model.Schedule;
import com.yogabot.model.Subscription;
import com.yogabot.service.BotService;
import com.yogabot.service.SupabaseService;
import com.yogabot.util.KeyboardUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramWebhookBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class BotController extends TelegramWebhookBot {

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.username}")
    private String botUsername;

    @Autowired
    private BotService botService;

    @Autowired
    private SupabaseService supabaseService;

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public String getBotPath() {
        return "";
    }

    public BotApiMethod<?> onWebhookUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            return handleMessage(update.getMessage());
        } else if (update.hasCallbackQuery()) {
            return handleCallbackQuery(update.getCallbackQuery());
        }
        return null;
    }

    // --- Message Handlers ---

    private BotApiMethod<?> handleMessage(Message message) {
        Long chatId = message.getChatId();
        String text = message.getText();
        Long userId = message.getFrom().getId();

        saveUser(message.getFrom());

        boolean isAdmin = botService.isAdmin(userId);

        switch (text) {
            case "/start":
                return sendWelcomeMessage(chatId, isAdmin);
            case "📅 Расписание":
                // Выводит расписание на 7 дней (уже реализовано в getWeeklySchedule)
                return sendSchedule(chatId, isAdmin);
            case "📋 Запись":
                if (isAdmin) {
                    // ИЗМЕНЕНО: Сразу показываем список на сегодня и завтра
                    String report = botService.getTodayTomorrowSubscriptions();
                    return sendMessage(chatId, report);
                } else {
                    return sendMessage(chatId, "⛔ Функция просмотра записей доступна только администратору.");
                }
            case "✏️ Редактирование":
                if (isAdmin) {
                    return sendEditOptions(chatId);
                } else {
                    return sendAccessDenied(chatId);
                }
            case "🔔 Уведомления вкл/выкл":
                if (isAdmin) {
                    return sendNotificationSettings(chatId);
                } else {
                    return sendAccessDenied(chatId);
                }
            case "✏️ Изменить":
                if (isAdmin) {
                    return sendEditScheduleMenu(chatId);
                } else {
                    return sendAccessDenied(chatId);
                }
            case "🗑 Удалить":
                if (isAdmin) {
                    return sendDeleteScheduleMenu(chatId);
                } else {
                    return sendAccessDenied(chatId);
                }
            case "🔙 Назад":
                return sendMainMenu(chatId, isAdmin);
            default:
                if (isAdmin && text.contains(":")) {
                    return handleAdminScheduleInput(chatId, text);
                }
                return sendMessage(chatId, "Неизвестная команда. Используйте кнопки меню.");
        }
    }

    private SendMessage handleAdminScheduleInput(Long chatId, String text) {
        return sendMessage(chatId, "Функция ручного ввода пока отключена. Используйте кнопки.");
    }

    // --- Send Methods ---

    private SendMessage sendWelcomeMessage(Long chatId, boolean isAdmin) {
        String welcomeText = "🧘 Добро пожаловать в Yoga Bot!\n\n" +
                "Я помогу вам с расписанием занятий и записью на тренировки.";
        return createMessage(chatId, welcomeText, KeyboardUtil.getMainKeyboard(isAdmin));
    }

    private SendMessage sendSchedule(Long chatId, boolean isAdmin) {
        String schedule = botService.getWeeklySchedule();
        return createMessage(chatId, schedule, KeyboardUtil.getMainKeyboard(isAdmin));
    }

    private SendMessage sendEditOptions(Long chatId) {
        String text = "✏️ Режим редактирования\n\nВыберите действие:";
        return createMessage(chatId, text, KeyboardUtil.getEditKeyboard());
    }

    private SendMessage sendNotificationSettings(Long chatId) {
        String text = "🔔 Уведомления работают автоматически в 16:00.";
        return sendMessage(chatId, text);
    }

    private SendMessage sendMainMenu(Long chatId, boolean isAdmin) {
        String text = "Главное меню:";
        return createMessage(chatId, text, KeyboardUtil.getMainKeyboard(isAdmin));
    }

    private SendMessage sendAccessDenied(Long chatId) {
        return sendMessage(chatId, "⛔ У вас нет доступа к этой функции.");
    }

    // --- Utility Methods ---

    private SendMessage sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        message.setParseMode("HTML"); // Важно для красивого текста
        return message;
    }

    private SendMessage createMessage(Long chatId, String text, ReplyKeyboard replyMarkup) {
        SendMessage message = sendMessage(chatId, text);
        message.setReplyMarkup(replyMarkup);
        return message;
    }

    private void saveUser(User telegramUser) {
        BotUser botUser = new BotUser();
        botUser.setTelegramId(telegramUser.getId());
        botUser.setFirstName(telegramUser.getFirstName());
        botUser.setLastName(telegramUser.getLastName());
        botUser.setUsername(telegramUser.getUserName());
        supabaseService.saveOrUpdateBotUser(botUser);
        System.out.println("✅ User saved from message: " + telegramUser.getId());
    }

    // --- Callback Query Handlers ---

    private BotApiMethod<?> handleCallbackQuery(CallbackQuery callbackQuery) {
        String data = callbackQuery.getData();
        Long userId = callbackQuery.getFrom().getId();
        Long chatId = callbackQuery.getMessage().getChatId();

        if (data.startsWith("subscribe_") || data.startsWith("unsubscribe_")) {
            return handleSubscription(data, userId, chatId, callbackQuery.getFrom());
        }
        else if (data.startsWith("edit_day_")) {
            return handleEditDay(data, chatId);
        }
        else if (data.startsWith("delete_day_")) {
            return handleDeleteDay(data, chatId);
        }
        else if (data.equals("back_to_edit")) {
            return sendEditOptions(chatId);
        }
        else if (data.equals("back_to_main")) {
            return sendMainMenu(chatId, botService.isAdmin(userId));
        }

        return null;
    }

    private SendMessage handleSubscription(String data, Long userId, Long chatId, User telegramUser) {
        try {
            String[] parts = data.split("_");
            String action = parts[0];
            String classType = parts[1].toUpperCase();
            Long scheduleId = Long.parseLong(parts[2]);

            saveUser(telegramUser);
            // Получаем дату расписания для записи
            Schedule schedule = supabaseService.getWeeklySchedule(LocalDate.now()).stream()
                    .filter(s -> s.getId() != null && s.getId().equals(scheduleId))
                    .findFirst()
                    .orElse(null);

            LocalDate classDate = (schedule != null) ? schedule.getDate() : LocalDate.now().plusDays(1);

            if (action.equals("subscribe")) {
                supabaseService.subscribeToClass(userId, scheduleId, classType, classDate);
                return sendMessage(chatId, "✅ Вы успешно записались на занятие!");
            } else {
                supabaseService.unsubscribeFromClass(userId, scheduleId, classType);
                return sendMessage(chatId, "❌ Запись на занятие отменена.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            return sendMessage(chatId, "❌ Произошла ошибка: " + e.getMessage());
        }
    }

    // Остальные методы редактирования (sendEditScheduleMenu, handleEditDay, и т.д.)
    // остаются без изменений или могут быть сокращены для краткости,
    // если вы их не меняли, оставьте их как есть в вашем файле.

    private SendMessage sendEditScheduleMenu(Long chatId) {
        // ... (Ваш существующий код)
        return sendMessage(chatId, "Функция редактирования в разработке.");
    }

    private SendMessage handleEditDay(String data, Long chatId) {
        return sendMessage(chatId, "Функция редактирования в разработке.");
    }

    private SendMessage sendDeleteScheduleMenu(Long chatId) {
        return sendMessage(chatId, "Функция удаления в разработке.");
    }

    private SendMessage handleDeleteDay(String data, Long chatId) {
        return sendMessage(chatId, "Функция удаления в разработке.");
    }
}