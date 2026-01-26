package com.lakhmann.budgetbot.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jobs")
public record JobsProperties(
        String cron,
        String zone,
        String jobToken
) {
}
