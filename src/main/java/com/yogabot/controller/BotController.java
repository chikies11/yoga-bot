package com.yogabot.controller;

import com.yogabot.model.BotUser;
import com.yogabot.model.Schedule;
import com.yogabot.service.BotService;
import com.yogabot.service.NotificationService;
import com.yogabot.service.SupabaseService;
import com.yogabot.util.KeyboardUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(BotController.class);

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.username}")
    private String botUsername;

    @Autowired
    private BotService botService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private SupabaseService supabaseService;

    @Override
    public String getBotUsername() { return botUsername; }

    @Override
    public String getBotToken() { return botToken; }

    @Override
    public String getBotPath() { return ""; }

    public BotApiMethod<?> onWebhookUpdateReceived(Update update) {
        try {
            if (update.hasMessage() && update.getMessage().hasText()) {
                return handleMessage(update.getMessage());
            } else if (update.hasCallbackQuery()) {
                return handleCallbackQuery(update.getCallbackQuery());
            }
        } catch (Exception e) {
            log.error("Error processing update", e);
        }
        return null;
    }

    private BotApiMethod<?> handleMessage(Message message) {
        Long chatId = message.getChatId();
        String text = message.getText();
        Long userId = message.getFrom().getId();

        try {
            saveUser(message.getFrom());
        } catch (Exception e) {
            log.error("Failed to save user", e);
        }

        boolean isAdmin = botService.isAdmin(userId);

        try {
            switch (text) {
                case "/start":
                    return sendWelcomeMessage(chatId, isAdmin);
                case "📅 Расписание":
                    return sendSchedule(chatId, isAdmin);
                case "📋 Запись":
                    if (isAdmin) {
                        return sendMessage(chatId, botService.getTodayTomorrowSubscriptions());
                    } else {
                        return sendMessage(chatId, "⛔ Функция просмотра записей доступна только администратору.");
                    }
                case "✏️ Редактирование":
                    return isAdmin ? sendEditOptions(chatId) : sendAccessDenied(chatId);
                case "🔔 Уведомления вкл/выкл":
                    if (isAdmin) {
                        // ИСПРАВЛЕНО: Теперь реально переключает
                        String status = notificationService.toggleNotifications();
                        return sendMessage(chatId, status);
                    } else {
                        return sendAccessDenied(chatId);
                    }
                case "✏️ Изменить":
                    return isAdmin ? sendEditScheduleMenu(chatId) : sendAccessDenied(chatId);
                case "🗑 Удалить":
                    return isAdmin ? sendDeleteScheduleMenu(chatId) : sendAccessDenied(chatId);
                case "🔙 Назад":
                    return sendMainMenu(chatId, isAdmin);
                default:
                    // Простое эхо, чтобы не игнорировать ввод (можно убрать)
                    return sendMessage(chatId, "Выберите действие в меню.");
            }
        } catch (Exception e) {
            log.error("Error handling message: " + text, e);
            return sendMessage(chatId, "❌ Произошла ошибка: " + e.getMessage());
        }
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
        return createMessage(chatId, "✏️ Режим редактирования\n\nВыберите действие:", KeyboardUtil.getEditKeyboard());
    }

    private SendMessage sendMainMenu(Long chatId, boolean isAdmin) {
        return createMessage(chatId, "Главное меню:", KeyboardUtil.getMainKeyboard(isAdmin));
    }

    private SendMessage sendAccessDenied(Long chatId) {
        return sendMessage(chatId, "⛔ У вас нет доступа к этой функции.");
    }

    // ИСПРАВЛЕНО: Генерация меню с кнопками дней
    private SendMessage sendEditScheduleMenu(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("✏️ Выберите день для изменения расписания (или выходного):");
        message.setReplyMarkup(botService.getScheduleKeyboard("edit_day_"));
        return message;
    }

    private SendMessage sendDeleteScheduleMenu(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("🗑 Выберите день для удаления (сброса) расписания:");
        message.setReplyMarkup(botService.getScheduleKeyboard("delete_day_"));
        return message;
    }

    // --- Utility Methods ---

    private SendMessage sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        message.setParseMode("HTML");
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
        log.info("✅ User saved/updated: {}", telegramUser.getId());
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
        else if (data.startsWith("confirm_delete_")) {
            return handleConfirmDelete(data, chatId);
        }
        else if (data.equals("cancel_delete")) {
            return sendDeleteScheduleMenu(chatId);
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
            log.error("Subscription error", e);
            return sendMessage(chatId, "❌ Ошибка: " + e.getMessage());
        }
    }

    // ИСПРАВЛЕНО: Обработка нажатия на день для редактирования
    private SendMessage handleEditDay(String data, Long chatId) {
        String dateStr = data.replace("edit_day_", "");
        return sendMessage(chatId, "✏️ Чтобы изменить расписание на <b>" + dateStr + "</b>, необходимо изменить код бота для поддержки текстового ввода.\n\n" +
                "<i>(В данной версии доступно только отображение меню дней)</i>");
    }

    // ИСПРАВЛЕНО: Обработка нажатия на день для удаления
    private SendMessage handleDeleteDay(String data, Long chatId) {
        String dateStr = data.replace("delete_day_", "");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();

        InlineKeyboardButton yes = new InlineKeyboardButton("✅ Да, удалить");
        yes.setCallbackData("confirm_delete_" + dateStr);

        InlineKeyboardButton no = new InlineKeyboardButton("❌ Отмена");
        no.setCallbackData("cancel_delete");

        row.add(yes);
        row.add(no);
        rows.add(row);
        markup.setKeyboard(rows);

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("🗑 Вы уверены, что хотите сбросить расписание на <b>" + dateStr + "</b>?");
        message.setReplyMarkup(markup);
        message.setParseMode("HTML");
        return message;
    }

    private SendMessage handleConfirmDelete(String data, Long chatId) {
        String dateStr = data.replace("confirm_delete_", "");
        try {
            supabaseService.deleteSchedule(LocalDate.parse(dateStr));
            return sendMessage(chatId, "✅ Расписание на " + dateStr + " успешно сброшено (отдых).");
        } catch (Exception e) {
            return sendMessage(chatId, "❌ Ошибка удаления: " + e.getMessage());
        }
    }
}