package com.lakhmann.budgetbot.telegram.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TelegramUpdate(
        @JsonProperty("message") Message message
) {
    public record Message(Chat chat, String text) {}
    public record Chat(Long id) {}
}
