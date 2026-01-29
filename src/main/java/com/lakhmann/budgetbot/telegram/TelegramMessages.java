package com.lakhmann.budgetbot.telegram;

public record TelegramMessages() {

    public static final String BALANCE_COMMAND_TEXT = "Получить актуальный баланс";
    public static final String READY_MESSAGE_TEXT = "Готово! Используй кнопку ниже";
    public static final String AVAILABLE_BALANCE_PREFIX = "Доступный баланс: ";
}
