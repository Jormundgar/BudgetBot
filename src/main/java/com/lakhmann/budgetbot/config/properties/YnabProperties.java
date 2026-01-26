package com.lakhmann.budgetbot.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ynab")
public record YnabProperties (
        String baseUrl,
        String token,
        String budgetId
) {}
