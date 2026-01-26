package com.lakhmann.budgetbot.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "currency")
public record CurrencyProperties(
        String symbol,
        int fractionDigits
) {
}
