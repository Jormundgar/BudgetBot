package com.lakhmann.budgetbot.telegram;

import com.lakhmann.budgetbot.config.properties.TelegramProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class TelegramClient {

    private final RestClient http;
    private final TelegramProperties telegramProperties;

    public TelegramClient(@Qualifier("telegramRestClient") RestClient http,
                          TelegramProperties telegramProperties) {
        this.http = http;
        this.telegramProperties = telegramProperties;
    }

    public void ensureBottomKeyboard(long chatId) {
        Map<String, Object> keyboard = Map.of(
                "keyboard", List.of(
                        List.of(Map.of("text", "Открыть Mini App", "web_app", Map.of("url", telegramProperties.miniappUrl())))
                ),
                "resize_keyboard", true,
                "one_time_keyboard", false
        );

        http.post()
                .uri("/sendMessage")
                .body(Map.of(
                        "chat_id", chatId,
                        "text", TelegramMessages.READY_MESSAGE_TEXT,
                        "reply_markup", keyboard
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
