package com.lakhmann.budgetbot.telegram;

public record TelegramMessages() {

    public static final String READY_MESSAGE_TEXT = "Готово! Используй кнопку Баланс ниже";
    public static final String AVAILABLE_BALANCE_PREFIX = "Доступный баланс: ";
}
