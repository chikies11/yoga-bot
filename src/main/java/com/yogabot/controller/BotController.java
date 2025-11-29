package com.yogabot.controller;

import com.yogabot.model.Schedule;
import com.yogabot.model.Subscription;
import com.yogabot.service.BotService;
import com.yogabot.service.SupabaseService;
import com.yogabot.util.KeyboardUtil;
import com.yogabot.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class BotController extends TelegramLongPollingBot {

    @Autowired
    private BotService botService;

    @Autowired
    private SupabaseService supabaseService;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.username}")
    private String botUsername;

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            handleMessage(update.getMessage());
        } else if (update.hasCallbackQuery()) {
            handleCallbackQuery(update.getCallbackQuery());
        }
    }

    private void handleMessage(Message message) {
        Long chatId = message.getChatId();
        String text = message.getText();
        Long userId = message.getFrom().getId();

        boolean isAdmin = botService.isAdmin(userId);

        switch (text) {
            case "/start":
                sendWelcomeMessage(chatId, isAdmin);
                break;
            case "📅 Расписание":
                sendSchedule(chatId);
                break;
            case "📋 Запись":
                if (isAdmin) {
                    sendMessage(chatId, "Функция просмотра записей будет доступна после настройки подписок");
                } else {
                    sendMessage(chatId, "Функция просмотра записей доступна только администратору.");
                }
                break;
            case "✏️ Редактирование":
                if (isAdmin) {
                    sendEditOptions(chatId);
                } else {
                    sendAccessDenied(chatId);
                }
                break;
            case "🔔 Уведомления вкл/выкл":
                sendMessage(chatId, "Функция управления уведомлениями в разработке.");
                break;
            case "✏️ Изменить":
                if (isAdmin) {
                    sendEditScheduleMenu(chatId);
                } else {
                    sendAccessDenied(chatId);
                }
                break;
            case "🗑 Удалить":
                if (isAdmin) {
                    sendDeleteScheduleMenu(chatId);
                } else {
                    sendAccessDenied(chatId);
                }
                break;
            case "🔙 Назад":
                sendMainMenu(chatId, isAdmin);
                break;
            default:
                sendMessage(chatId, "Неизвестная команда. Используйте кнопки меню.");
        }
    }

    private void handleSubscription(String data, Long userId, Long chatId) {
        try {
            String[] parts = data.split("_");
            String action = parts[0]; // subscribe or unsubscribe
            String classType = parts[1].toUpperCase(); // MORNING or EVENING
            Long scheduleId = Long.parseLong(parts[2]);

            // Сначала сохраняем/обновляем пользователя
            User user = supabaseService.getUserByTelegramId(userId);
            if (user == null) {
                // Создаем нового пользователя
                user = new User();
                user.setTelegramId(userId);
                user.setAdmin(false); // по умолчанию не админ
                supabaseService.saveUser(user);

                // Получаем пользователя с ID из базы
                user = supabaseService.getUserByTelegramId(userId);
            }

            if (user == null) {
                sendMessage(chatId, "❌ Ошибка: не удалось создать пользователя");
                return;
            }

            LocalDate classDate = LocalDate.now().plusDays(1); // занятия всегда на завтра

            if (action.equals("subscribe")) {
                supabaseService.subscribeToClass(user.getId(), scheduleId, classType, classDate);
                sendMessage(chatId, "✅ Вы успешно записались на занятие!");
            } else {
                supabaseService.unsubscribeFromClass(user.getId(), scheduleId, classType);
                sendMessage(chatId, "❌ Запись на занятие отменена.");
            }
        } catch (Exception e) {
            sendMessage(chatId, "❌ Произошла ошибка при обработке запроса");
            System.err.println("Error handling subscription: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void sendWelcomeMessage(Long chatId, boolean isAdmin) {
        String welcomeText = "🧘 Добро пожаловать в Yoga Bot!\n\n" +
                "Я помогу вам с расписанием занятий и записью на тренировки.";

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(welcomeText);
        message.setReplyMarkup(KeyboardUtil.getMainKeyboard(isAdmin));

        executeMessage(message);
    }

    private void sendSchedule(Long chatId) {
        String schedule = botService.getWeeklySchedule();
        sendMessage(chatId, schedule);
    }

    private void sendEditOptions(Long chatId) {
        String text = "✏️ Режим редактирования\n\n" +
                "Выберите действие для работы с расписанием:";

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        message.setReplyMarkup(KeyboardUtil.getEditKeyboard());

        executeMessage(message);
    }

    private void toggleNotifications(Long chatId) {
        // Реализация включения/выключения уведомлений
        sendMessage(chatId, "Функция управления уведомлениями в разработке.");
    }

    private void sendMainMenu(Long chatId, boolean isAdmin) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("Главное меню:");
        message.setReplyMarkup(KeyboardUtil.getMainKeyboard(isAdmin));

        executeMessage(message);
    }

    private void sendAccessDenied(Long chatId) {
        sendMessage(chatId, "⛔ У вас нет доступа к этой функции.");
    }

    private void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        executeMessage(message);
    }

    private void executeMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public BotApiMethod<?> onWebhookUpdateReceived(Update update) {
        onUpdateReceived(update);
        return null;
    }

    private void sendSubscriptionsMenu(Long chatId) {
        try {
            LocalDate startOfWeek = LocalDate.now().minusDays(LocalDate.now().getDayOfWeek().getValue() - 1);
            List<Schedule> schedules = supabaseService.getWeeklySchedule(startOfWeek);

            if (schedules.isEmpty()) {
                sendMessage(chatId, "Расписание не найдено.");
                return;
            }

            InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            for (Schedule schedule : schedules) {
                String dayName = botService.getRussianDayName(schedule.getDate().getDayOfWeek());

                if (schedule.getMorningTime() != null) {
                    List<InlineKeyboardButton> morningRow = new ArrayList<>();
                    InlineKeyboardButton morningButton = new InlineKeyboardButton();
                    morningButton.setText("📋 " + dayName + " Утро");
                    morningButton.setCallbackData("view_morning_" + schedule.getId());
                    morningRow.add(morningButton);
                    rows.add(morningRow);
                }

                if (schedule.getEveningTime() != null) {
                    List<InlineKeyboardButton> eveningRow = new ArrayList<>();
                    InlineKeyboardButton eveningButton = new InlineKeyboardButton();
                    eveningButton.setText("📋 " + dayName + " Вечер");
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

            executeMessage(message);

        } catch (Exception e) {
            sendMessage(chatId, "Ошибка при загрузке расписания: " + e.getMessage());
        }
    }

    private void sendEditScheduleMenu(Long chatId) {
        try {
            // Получаем расписание на текущую неделю
            LocalDate startOfWeek = LocalDate.now().minusDays(LocalDate.now().getDayOfWeek().getValue() - 1);
            List<Schedule> schedules = supabaseService.getWeeklySchedule(startOfWeek);

            if (schedules.isEmpty()) {
                sendMessage(chatId, "Расписание не найдено. Сначала инициализируйте расписание.");
                return;
            }

            // Создаем инлайн-кнопки для выбора дня
            InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM");

            for (Schedule schedule : schedules) {
                // Используем botService для получения русского названия дня
                String dayName = botService.getRussianDayName(schedule.getDate().getDayOfWeek());
                String buttonText = dayName + " (" + schedule.getDate().format(formatter) + ")";

                List<InlineKeyboardButton> row = new ArrayList<>();
                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText(buttonText);
                button.setCallbackData("edit_day_" + schedule.getDate());

                row.add(button);
                rows.add(row);
            }

            // Кнопка "Назад"
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

            executeMessage(message);

        } catch (Exception e) {
            sendMessage(chatId, "Ошибка при загрузке расписания: " + e.getMessage());
        }
    }

    private void sendDeleteScheduleMenu(Long chatId) {
        try {
            LocalDate startOfWeek = LocalDate.now().minusDays(LocalDate.now().getDayOfWeek().getValue() - 1);
            List<Schedule> schedules = supabaseService.getWeeklySchedule(startOfWeek);

            if (schedules.isEmpty()) {
                sendMessage(chatId, "Расписание не найдено.");
                return;
            }

            InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM");

            for (Schedule schedule : schedules) {
                // Используем botService для получения русского названия дня
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

            executeMessage(message);

        } catch (Exception e) {
            sendMessage(chatId, "Ошибка при загрузке расписания: " + e.getMessage());
        }
    }

    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        String data = callbackQuery.getData();
        Long userId = callbackQuery.getFrom().getId();
        Long chatId = callbackQuery.getMessage().getChatId();

        if (data.startsWith("subscribe_") || data.startsWith("unsubscribe_")) {
            handleSubscription(data, userId, chatId);
        }
        else if (data.startsWith("view_morning_") || data.startsWith("view_evening_")) {
            handleViewSubscriptions(data, chatId);
        }
        else if (data.startsWith("edit_day_")) {
            handleEditDay(data, chatId);
        }
        else if (data.startsWith("delete_day_")) {
            handleDeleteDay(data, chatId);
        }
        else if (data.equals("back_to_edit")) {
            sendEditOptions(chatId);
        }
        else if (data.equals("back_to_main")) {
            sendMainMenu(chatId, botService.isAdmin(userId));
        }
    }

    private void handleViewSubscriptions(String data, Long chatId) {
        try {
            String[] parts = data.split("_");
            String classType = parts[1].toUpperCase(); // MORNING or EVENING
            Long scheduleId = Long.parseLong(parts[2]);

            List<Subscription> subscriptions = supabaseService.getSubscriptionsForClass(scheduleId, classType);

            if (subscriptions.isEmpty()) {
                sendMessage(chatId, "На это занятие пока никто не записался.");
                return;
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

            sendMessage(chatId, sb.toString());

        } catch (Exception e) {
            sendMessage(chatId, "Ошибка при загрузке записей: " + e.getMessage());
        }
    }

    private void handleEditDay(String data, Long chatId) {
        try {
            String dateStr = data.replace("edit_day_", "");
            LocalDate date = LocalDate.parse(dateStr);

            Schedule schedule = supabaseService.getScheduleByDate(date);

            if (schedule == null) {
                sendMessage(chatId, "Расписание на выбранную дату не найдено.");
                return;
            }

            // Используем botService для получения русского названия дня
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

            sendMessage(chatId, messageText);

        } catch (Exception e) {
            sendMessage(chatId, "Ошибка при загрузке расписания: " + e.getMessage());
        }
    }

    private void handleDeleteDay(String data, Long chatId) {
        try {
            String dateStr = data.replace("delete_day_", "");
            LocalDate date = LocalDate.parse(dateStr);

            Schedule schedule = supabaseService.getScheduleByDate(date);

            if (schedule == null) {
                sendMessage(chatId, "Расписание на выбранную дату не найдено.");
                return;
            }

            // Создаем кнопки подтверждения удаления
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

            // Используем botService для получения русского названия дня
            String dayName = botService.getRussianDayName(date.getDayOfWeek());
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

            SendMessage message = new SendMessage();
            message.setChatId(chatId.toString());
            message.setText("🗑 Вы уверены, что хотите удалить расписание на " +
                    dayName + " (" + date.format(formatter) + ")?");
            message.setReplyMarkup(keyboardMarkup);

            executeMessage(message);

        } catch (Exception e) {
            sendMessage(chatId, "Ошибка при удалении расписания: " + e.getMessage());
        }
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
        String dayName = botService.getRussianDayName(schedule.getDate().getDayOfWeek());
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

        // Добавляем инлайн-кнопки для записи
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
}