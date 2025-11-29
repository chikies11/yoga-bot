package com.yogabot.controller;

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
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDate;

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

        // Сохраняем информацию о пользователе
        User user = new User(userId, message.getFrom().getFirstName(),
                message.getFrom().getLastName(), message.getFrom().getUserName(),
                botService.isAdmin(userId));
        supabaseService.saveUser(user);

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
                    sendSubscriptionsMenu(chatId);
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
                if (isAdmin) {
                    toggleNotifications(chatId);
                } else {
                    sendAccessDenied(chatId);
                }
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

    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        String data = callbackQuery.getData();
        Long userId = callbackQuery.getFrom().getId();
        Long chatId = callbackQuery.getMessage().getChatId();
        Integer messageId = callbackQuery.getMessage().getMessageId();

        if (data.startsWith("subscribe_") || data.startsWith("unsubscribe_")) {
            handleSubscription(data, userId, chatId, messageId);
        }
    }

    private void handleSubscription(String data, Long userId, Long chatId, Integer messageId) {
        String[] parts = data.split("_");
        String action = parts[0]; // subscribe or unsubscribe
        String classType = parts[1].toUpperCase(); // MORNING or EVENING
        Long scheduleId = Long.parseLong(parts[2]);

        User user = supabaseService.getUserByTelegramId(userId);
        if (user == null) {
            sendMessage(chatId, "Ошибка: пользователь не найден.");
            return;
        }

        if (action.equals("subscribe")) {
            supabaseService.subscribeToClass(user.getId(), scheduleId, classType,
                    LocalDate.now().plusDays(1));
            sendMessage(chatId, "✅ Вы успешно записались на занятие!");
        } else {
            supabaseService.unsubscribeFromClass(user.getId(), scheduleId, classType);
            sendMessage(chatId, "❌ Запись на занятие отменена.");
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
        String text = "📋 Просмотр записей\n\n" +
                "Выберите день и тип занятия для просмотра записавшихся:";
        sendMessage(chatId, text);
        // Здесь можно добавить инлайн-кнопки для выбора дня/типа занятия
    }

    private void sendEditScheduleMenu(Long chatId) {
        String text = "✏️ Редактирование расписания\n\n" +
                "Выберите день для редактирования:";
        sendMessage(chatId, text);
        // Здесь можно добавить инлайн-кнопки для выбора дня
    }

    private void sendDeleteScheduleMenu(Long chatId) {
        String text = "🗑 Удаление занятия\n\n" +
                "Выберите день и тип занятия для удаления:";
        sendMessage(chatId, text);
        // Здесь можно добавить инлайн-кнопки для выбора дня/типа занятия
    }
}