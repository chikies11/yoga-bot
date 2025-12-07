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
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard; // <-- Добавлен импорт
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
        return ""; // Соответствует PostMapping("/") в WebhookController
    }

    // ВАЖНО: onUpdateReceived УДАЛЕН ИЛИ ОСТАВЛЕН БЕЗ @Override,
    // чтобы не конфликтовать с final-методом в TelegramWebhookBot.

    /**
     * Основная логика обработки обновлений Webhook.
     */
    public BotApiMethod<?> onWebhookUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            return handleMessage(update.getMessage());
        } else if (update.hasCallbackQuery()) {
            return handleCallbackQuery(update.getCallbackQuery());
        }

        // Возвращаем null, если обновление не требует ответа
        return null;
    }

    // --- Message Handlers (Возвращают SendMessage) ---

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
                return sendSchedule(chatId, isAdmin);
            case "📋 Запись":
                if (isAdmin) {
                    return sendSubscriptionsMenu(chatId);
                } else {
                    return sendMessage(chatId, "Функция просмотра записей доступна только администратору.");
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
                if (isAdmin) {
                    return handleAdminScheduleInput(chatId, text);
                }
                return sendMessage(chatId, "Неизвестная команда. Используйте кнопки меню.");
        }
    }

    private SendMessage handleAdminScheduleInput(Long chatId, String text) {
        // Здесь должна быть логика парсинга и сохранения расписания
        return sendMessage(chatId, "Неизвестная команда. Используйте кнопки меню.");
    }


    // --- Send Methods (Возвращают SendMessage) ---

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
        String text = "✏️ Режим редактирования\n\n" +
                "Выберите действие для работы с расписанием:";
        return createMessage(chatId, text, KeyboardUtil.getEditKeyboard());
    }

    private SendMessage sendNotificationSettings(Long chatId) {
        String text = "🔔 Управление уведомлениями\n\n" +
                "Автоматические уведомления отправляются ежедневно в:\n" +
                "• 16:00 - уведомление о завтрашних занятиях\n" +
                "• 16:01 - дополнительная проверка\n\n" +
                "Уведомления включены и работают автоматически.";
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
        return message;
    }

    private SendMessage createMessage(Long chatId, String text, ReplyKeyboard replyMarkup) {
        SendMessage message = sendMessage(chatId, text);
        message.setReplyMarkup(replyMarkup);
        return message;
    }

    private void saveUser(User telegramUser) {
        Long userId = telegramUser.getId();
        BotUser botUser = supabaseService.getBotUserByTelegramId(userId);
        if (botUser == null) {
            botUser = new BotUser();
            botUser.setTelegramId(userId);
            botUser.setFirstName(telegramUser.getFirstName());
            botUser.setLastName(telegramUser.getLastName());
            botUser.setUsername(telegramUser.getUserName());
            supabaseService.saveOrUpdateBotUser(botUser);
            System.out.println("✅ User saved from message: " + userId);
        }
    }


    // --- Callback Query Handlers (Возвращают BotApiMethod<?>) ---

    private BotApiMethod<?> handleCallbackQuery(CallbackQuery callbackQuery) {
        String data = callbackQuery.getData();
        Long userId = callbackQuery.getFrom().getId();
        Long chatId = callbackQuery.getMessage().getChatId();

        org.telegram.telegrambots.meta.api.objects.User telegramUser = callbackQuery.getFrom();

        if (data.startsWith("subscribe_") || data.startsWith("unsubscribe_")) {
            return handleSubscription(data, userId, chatId, telegramUser);
        }
        else if (data.startsWith("view_morning_") || data.startsWith("view_evening_")) {
            return handleViewSubscriptions(data, chatId);
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

    private SendMessage handleSubscription(String data, Long userId, Long chatId,
                                           org.telegram.telegrambots.meta.api.objects.User telegramUser) {
        try {
            System.out.println("🔄 Handling subscription: " + data + " for user: " + userId);

            String[] parts = data.split("_");
            String action = parts[0];
            String classType = parts[1].toUpperCase();
            Integer scheduleId = Integer.parseInt(parts[2]);

            saveUser(telegramUser);

            LocalDate classDate = LocalDate.now().plusDays(1);

            if (action.equals("subscribe")) {
                supabaseService.subscribeToClass(userId, scheduleId.longValue(), classType, classDate);
                System.out.println("✅ Subscribed to class: " + scheduleId + " - " + classType);
                return sendMessage(chatId, "✅ Вы успешно записались на занятие!");
            } else {
                supabaseService.unsubscribeFromClass(userId, scheduleId.longValue(), classType);
                System.out.println("✅ Unsubscribed from class: " + scheduleId + " - " + classType);
                return sendMessage(chatId, "❌ Запись на занятие отменена.");
            }

        } catch (Exception e) {
            System.err.println("❌ Error in handleSubscription: " + e.getMessage());
            e.printStackTrace();
            return sendMessage(chatId, "❌ Произошла ошибка при обработке запроса.");
        }
    }

    private SendMessage sendSubscriptionsMenu(Long chatId) {
        try {
            boolean isAdmin = botService.isAdmin(chatId);
            if (!isAdmin) {
                return sendMessage(chatId, "⛔ Функция просмотра записей доступна только администратору.");
            }

            LocalDate startOfWeek = LocalDate.now();
            List<Schedule> schedules = supabaseService.getWeeklySchedule(startOfWeek);

            if (schedules.isEmpty()) {
                return sendMessage(chatId, "Расписание не найдено.");
            }

            InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            for (Schedule schedule : schedules) {
                String dayName = botService.getRussianDayName(schedule.getDate().getDayOfWeek());

                if (schedule.getMorningTime() != null && schedule.isActive()) {
                    List<InlineKeyboardButton> morningRow = new ArrayList<>();
                    InlineKeyboardButton morningButton = new InlineKeyboardButton();
                    morningButton.setText("📋 " + dayName + " Утро (" + schedule.getMorningTime() + ")");
                    morningButton.setCallbackData("view_morning_" + schedule.getId());
                    morningRow.add(morningButton);
                    rows.add(morningRow);
                }

                if (schedule.getEveningTime() != null && schedule.isActive()) {
                    List<InlineKeyboardButton> eveningRow = new ArrayList<>();
                    InlineKeyboardButton eveningButton = new InlineKeyboardButton();
                    eveningButton.setText("📋 " + dayName + " Вечер (" + schedule.getEveningTime() + ")");
                    eveningButton.setCallbackData("view_evening_" + schedule.getId());
                    eveningRow.add(eveningButton);
                    rows.add(eveningRow);
                }
            }

            List<InlineKeyboardButton> backRow = new ArrayList<>();
            InlineKeyboardButton backButton = new InlineKeyboardButton();
            backButton.setText("🔙 Назад");
            backButton.setCallbackData("back_to_main");
            backRow.add(backButton);
            rows.add(backRow);

            keyboardMarkup.setKeyboard(rows);

            SendMessage message = new SendMessage();
            message.setChatId(chatId.toString());
            message.setText("📋 Выберите занятие для просмотра записей:");
            message.setReplyMarkup(keyboardMarkup);

            return message;

        } catch (Exception e) {
            return sendMessage(chatId, "Ошибка при загрузке расписания: " + e.getMessage());
        }
    }

    private SendMessage handleViewSubscriptions(String data, Long chatId) {
        try {
            String[] parts = data.split("_");
            String classType = parts[1].toUpperCase();
            Long scheduleId = Long.parseLong(parts[2]);

            List<Subscription> subscriptions = supabaseService.getSubscriptionsForClass(scheduleId, classType);

            if (subscriptions.isEmpty()) {
                return sendMessage(chatId, "На это занятие пока никто не записался.");
            }

            StringBuilder sb = new StringBuilder();
            String classTime = classType.equals("MORNING") ? "утреннее" : "вечернее";
            sb.append("📋 Список записавшихся на ").append(classTime).append(" занятие:\n\n");

            for (int i = 0; i < subscriptions.size(); i++) {
                Subscription subscription = subscriptions.get(i);

                BotUser user = supabaseService.getBotUserByTelegramId(subscription.getTelegramId());

                if (user != null) {
                    String userName = user.getDisplayName();
                    sb.append(i + 1).append(". ").append(userName).append("\n");
                } else {
                    sb.append(i + 1).append(". Пользователь ID: ").append(subscription.getTelegramId()).append("\n");
                }
            }

            return sendMessage(chatId, sb.toString());

        } catch (Exception e) {
            e.printStackTrace();
            return sendMessage(chatId, "Ошибка при загрузке записей: " + e.getMessage());
        }
    }

    private SendMessage sendEditScheduleMenu(Long chatId) {
        try {
            LocalDate startDay = LocalDate.now();
            List<Schedule> schedules = supabaseService.getWeeklySchedule(startDay);

            if (schedules.isEmpty()) {
                return sendMessage(chatId, "Расписание не найдено. Сначала инициализируйте расписание.");
            }

            InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM");

            for (Schedule schedule : schedules) {
                String dayName = botService.getRussianDayName(schedule.getDate().getDayOfWeek());
                String buttonText = dayName + " (" + schedule.getDate().format(formatter) + ")";

                List<InlineKeyboardButton> row = new ArrayList<>();
                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText(buttonText);
                button.setCallbackData("edit_day_" + schedule.getDate());

                row.add(button);
                rows.add(row);
            }

            List<InlineKeyboardButton> backRow = new ArrayList<>();
            InlineKeyboardButton backButton = new InlineKeyboardButton();
            backButton.setText("🔙 Назад");
            backButton.setCallbackData("back_to_edit");
            backRow.add(backButton);
            rows.add(backRow);

            keyboardMarkup.setKeyboard(rows);

            SendMessage message = new SendMessage();
            message.setChatId(chatId.toString());
            message.setText("✏️ Выберите день для редактирования:");
            message.setReplyMarkup(keyboardMarkup);

            return message;

        } catch (Exception e) {
            return sendMessage(chatId, "Ошибка при загрузке расписания: " + e.getMessage());
        }
    }

    private SendMessage handleEditDay(String data, Long chatId) {
        try {
            String dateStr = data.replace("edit_day_", "");
            LocalDate date = LocalDate.parse(dateStr);

            Schedule schedule = supabaseService.getScheduleByDate(date);

            if (schedule == null) {
                return sendMessage(chatId, "Расписание на выбранную дату не найдено.");
            }

            String dayName = botService.getRussianDayName(date.getDayOfWeek());
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

            String messageText = "✏️ Редактирование расписания:\n\n" +
                    "🗓 " + dayName + " (" + date.format(formatter) + ")\n\n" +
                    "Текущее расписание:\n";

            if (schedule.getMorningTime() != null) {
                messageText += "🌅 Утро: " + schedule.getMorningTime() + " - " + schedule.getMorningClass() + "\n";
            }

            if (schedule.getEveningTime() != null) {
                messageText += "🌇 Вечер: " + schedule.getEveningTime() + " - " + schedule.getEveningClass() + "\n";
            }

            messageText += "\nДля изменения отправьте новое расписание в формате:\n" +
                    "Утро: 8:00 МАЙСОР КЛАСС\n" +
                    "Вечер: 17:00 МАЙСОР КЛАСС\n\n" +
                    "Или отправьте 'Отдых' для выходного дня.";

            return sendMessage(chatId, messageText);

        } catch (Exception e) {
            return sendMessage(chatId, "Ошибка при загрузке расписания: " + e.getMessage());
        }
    }

    private SendMessage sendDeleteScheduleMenu(Long chatId) {
        try {
            LocalDate startDay = LocalDate.now();
            List<Schedule> schedules = supabaseService.getWeeklySchedule(startDay);

            if (schedules.isEmpty()) {
                return sendMessage(chatId, "Расписание не найдено.");
            }

            InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM");

            for (Schedule schedule : schedules) {
                String dayName = botService.getRussianDayName(schedule.getDate().getDayOfWeek());
                String buttonText = dayName + " (" + schedule.getDate().format(formatter) + ")";

                List<InlineKeyboardButton> row = new ArrayList<>();
                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText(buttonText);
                button.setCallbackData("delete_day_" + schedule.getDate());

                row.add(button);
                rows.add(row);
            }

            List<InlineKeyboardButton> backRow = new ArrayList<>();
            InlineKeyboardButton backButton = new InlineKeyboardButton();
            backButton.setText("🔙 Назад");
            backButton.setCallbackData("back_to_edit");
            backRow.add(backButton);
            rows.add(backRow);

            keyboardMarkup.setKeyboard(rows);

            SendMessage message = new SendMessage();
            message.setChatId(chatId.toString());
            message.setText("🗑 Выберите день для удаления:");
            message.setReplyMarkup(keyboardMarkup);

            return message;

        } catch (Exception e) {
            return sendMessage(chatId, "Ошибка при загрузке расписания: " + e.getMessage());
        }
    }

    private SendMessage handleDeleteDay(String data, Long chatId) {
        try {
            String dateStr = data.replace("delete_day_", "");
            LocalDate date = LocalDate.parse(dateStr);

            Schedule schedule = supabaseService.getScheduleByDate(date);

            if (schedule == null) {
                return sendMessage(chatId, "Расписание на выбранную дату не найдено.");
            }

            InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            List<InlineKeyboardButton> confirmRow = new ArrayList<>();
            InlineKeyboardButton confirmButton = new InlineKeyboardButton();
            confirmButton.setText("✅ Да, удалить");
            confirmButton.setCallbackData("confirm_delete_" + date);

            InlineKeyboardButton cancelButton = new InlineKeyboardButton();
            cancelButton.setText("❌ Отмена");
            cancelButton.setCallbackData("cancel_delete");

            confirmRow.add(confirmButton);
            confirmRow.add(cancelButton);
            rows.add(confirmRow);

            keyboardMarkup.setKeyboard(rows);

            String dayName = botService.getRussianDayName(date.getDayOfWeek());
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

            SendMessage message = new SendMessage();
            message.setChatId(chatId.toString());
            message.setText("🗑 Вы уверены, что хотите удалить расписание на " +
                    dayName + " (" + date.format(formatter) + ")?");
            message.setReplyMarkup(keyboardMarkup);

            return message;

        } catch (Exception e) {
            return sendMessage(chatId, "Ошибка при удалении расписания: " + e.getMessage());
        }
    }
}