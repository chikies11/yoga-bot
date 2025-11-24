package com.yogabot.util;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import java.util.ArrayList;
import java.util.List;

public class KeyboardUtil {

    public static ReplyKeyboardMarkup getMainKeyboard(boolean isAdmin) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setSelective(true);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboard = new ArrayList<>();

        // Первый ряд
        KeyboardRow row1 = new KeyboardRow();
        row1.add("📅 Расписание");
        row1.add("📋 Запись");
        keyboard.add(row1);

        // Второй ряд (только для админа)
        if (isAdmin) {
            KeyboardRow row2 = new KeyboardRow();
            row2.add("✏️ Редактирование");
            row2.add("🔔 Уведомления вкл/выкл");
            keyboard.add(row2);
        }

        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    public static ReplyKeyboardMarkup getEditKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setSelective(true);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("✏️ Изменить");
        row1.add("🗑 Удалить");

        KeyboardRow row2 = new KeyboardRow();
        row2.add("🔙 Назад");

        keyboard.add(row1);
        keyboard.add(row2);

        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    public static ReplyKeyboardRemove removeKeyboard() {
        return new ReplyKeyboardRemove(true);
    }
}