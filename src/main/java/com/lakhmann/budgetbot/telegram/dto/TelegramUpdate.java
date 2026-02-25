package com.lakhmann.budgetbot.telegram.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TelegramUpdate(
        @JsonProperty("message") Message message
) {
    public record Message(Chat chat, String text, @JsonProperty("web_app_data") WebAppData webAppData) {}
    public record Chat(Long id) {}
    public record WebAppData(String data, @JsonProperty("button_text") String buttonText) {}
}
