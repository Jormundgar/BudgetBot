package com.lakhmann.budgetbot.telegram;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class TelegramClient {

    private final RestClient http;
    public TelegramClient(@Qualifier("telegramRestClient") RestClient http) {
        this.http = http;
    }

    public void ensureBottomKeyboard(long chatId) {
        http.post()
                .uri("/sendMessage")
                .body(Map.of(
                        "chat_id", chatId,
                        "text", TelegramMessages.READY_MESSAGE_TEXT
                ))
                .retrieve()
                .toBodilessEntity();
    }

    public void sendPlainMessage(long chatId, String text) {
        http.post()
                .uri("/sendMessage")
                .body(Map.of(
                        "chat_id", chatId,
                        "text", text
                ))
                .retrieve()
                .toBodilessEntity();
    }
}
