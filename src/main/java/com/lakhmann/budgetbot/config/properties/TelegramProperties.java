package com.lakhmann.budgetbot.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Arrays;

@ConfigurationProperties(prefix = "telegram")
public record TelegramProperties(
        String baseUrl,
        String botToken,
        String recipients,
        String webhookSecret
) {
    public List<Long> recipientIds() {
        if (recipients == null || recipients.isBlank()) return List.of();
        return Arrays.stream(recipients.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(Long::parseLong)
                .toList();
    }
}
